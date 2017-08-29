package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int[] device_list= { 5554, 5556, 5558, 5560, 5562};
    static final String[] multicast_ports = { REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2,
            REMOTE_PORT3, REMOTE_PORT4};
    static final int SERVER_PORT = 10000;
    static int seqNo=0, deviceId, TIMEOUT = 2000, INIT_CAPACITY = 10;
    int count = 0;
    static PriorityQueue<Message> msg_queue = new PriorityQueue<Message>(INIT_CAPACITY);
    static SparseIntArray msg_sequencer = new SparseIntArray();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         *  We will be using the same hack that we have been using in the previous assignments
         *  to get the PORT Number of the device.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        deviceId = Integer.parseInt(portStr);

        // Initialise sequencer for each client to 0. It holds the current count for each client
        for(int dev_id: device_list) {
            msg_sequencer.put(dev_id, 0);
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e("IOException", "Server Socket Error!");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //Including 'Enter' Key Submit on Text field
        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });

        // Submit by clicking SEND Button
        Button b = (Button) findViewById(R.id.button4);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Message msg, temp;
            int clientId=0;
            int proposedPriority;
            ObjectInputStream in_stream;
            ObjectOutputStream out_stream;
            Iterator<Message> msg_q_iter;
            while(true) {
                Socket serverListener = null;
                try {
                    serverListener = serverSocket.accept();
                    in_stream = new ObjectInputStream(serverListener.getInputStream());
                    out_stream = new ObjectOutputStream(serverListener.getOutputStream());
                    /*
                     * Our server shall be a full duplex socket, which shall read the incoming
                     * message, and multicast it to other clients, if delivery status is true,
                     * else as per ISIS algorithm, it will be buffered.
                     */
                    msg = (Message)in_stream.readObject();  // Receive the Message object from client
                    clientId = msg.getDeviceId();           // Get to know the client sending msg
                    proposedPriority = msg_sequencer.get(msg.getDeviceId())+1;
                    //proposed priority is sequence number of last event + 1

                    //If proposed priority is greater than initial priority of obtained message,
                    // we always assign the highest priority available
                    if(proposedPriority > msg.getMsgPriority()){
                        msg.setProposedPriority(proposedPriority);
                        msg.setDeviceId(deviceId);
                    }else{
                        msg.setProposedPriority(msg.getMsgPriority());
                    }
                    msg.setDeliveryStatus(false);
                    msg_queue.add(msg);
                    out_stream.writeObject(msg);
                    out_stream.flush();
                    //The modified message is added to the buffer queue above

                    /*  Next, iterate over all the messages present in the buffer, and check which
                     *  messages are eligible to be multicasted after the last event of receiving
                     *  message. If we have the same message present already in the queue,remove it,
                     *  change its delivery status to True and add in the message queue
                     *  Note that the presence of exact same message is ensured doubly by also
                     *  ensuring that device linked to the message object is indeed same.
                     */
                    msg = (Message)in_stream.readObject();
                    msg_q_iter = msg_queue.iterator();
                    while(msg_q_iter.hasNext()){
                        temp = msg_q_iter.next();
                        if(temp.getMsgPriority() == msg.getMsgPriority()
                                && temp.getDeviceId() == msg.getDeviceId()){
                            msg_queue.remove(temp);
                            msg.setDeliveryStatus(true);
                            msg_queue.add(msg);
                            break;
                        }
                    }
                    while(msg_queue.peek()!=null && msg_queue.peek().getDeliveryStatus()){
                        /*
                         *  Begin by checking if the queue is empty, peek helps to check head
                         *  element without removing it.
                         *  If queue is not empty, pop the top element and write to a file,
                         *  which is our database in this case as in PA 2a.
                         */

                        msg = msg_queue.poll();
                        int key = count++;

                        /*  Group Messenger Provider handles the content provider part of our
                         *  implementation. The Content Provider implemented is exactly the same as
                         *  in previous Assignment PA-2a. We are using a file based content provider
                         *  to store <key,value> pairs; where 'key' represents is the message id and
                         *  'value' is the message string.
                         */
                        ContentResolver contentResolver = getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
                        uriBuilder.scheme("content");

                        Uri uri = uriBuilder.build();
                        contentValues.put("key", Integer.toString(key));
                        contentValues.put("value", msg.getMessage());
                        contentResolver.insert(uri, contentValues);
                    }
                    publishProgress(msg.getMessage(), Integer.toString(msg.getConsensusPriority()));

                }catch (EOFException e){
                    Log.e("S_EOF_EXCEPTION", "EOFException: " + e.toString());
                }
                catch (StreamCorruptedException e){
                    Log.e("S_STREAM_CORRUPT", "StreamCorruptedException: " + e.toString());
                }
                catch (SocketTimeoutException e){
                    Log.e("S_SOCKET_TIMEOUT", "SocketTimeoutException: " + e.toString());
                }
                catch (IOException e) {
                    Log.e("S_IO_EXCEPTION", "IOException: " + e.toString());
                }
                catch (Exception e){
                    Log.e("S_GEN_EXCEPTION","Exception: " + e.toString());
                }
                finally{
                    /* Always a good practice to clean up the queue in case of failure */
                    Iterator<Message> msg_q_iter2 = msg_queue.iterator();
                    Message del_msg;
                    while(msg_q_iter2.hasNext()){
                        del_msg= msg_q_iter2.next();
                        if(del_msg.getDeviceId() == clientId) {
                            msg_queue.remove(del_msg);
                        }
                    }
                }

            }
        }

        @Override
        protected void onProgressUpdate(String... messages) {
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(messages[0] + "\n");
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Socket sockets[] = new Socket[5];
            String msg_out = null;
            int count=0;
            // For any event, every client first increments its current sequence number in FIFO
            seqNo++;
            ObjectInputStream in_streams[] = new ObjectInputStream[5];
            ObjectOutputStream out_streams[] = new ObjectOutputStream[5];
            List<Message> messageList = new ArrayList<Message>();
            ObjectInputStream in_stream;
            ObjectOutputStream out_stream;
            Socket socket;
            Message message;
            for (String port: multicast_ports) {
                try {
                    sockets[count] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    socket = sockets[count];
                    out_streams[count] = new ObjectOutputStream(socket.getOutputStream());
                    in_streams[count] = new ObjectInputStream(socket.getInputStream());
                    out_stream = out_streams[count];
                    in_stream = in_streams[count];
                    socket.setSoTimeout(TIMEOUT);
                    msg_out = msgs[0];
                    message = new Message(msg_out,seqNo, deviceId);
                    out_stream.writeObject(message);
                    out_stream.flush();
                    message = (Message)in_stream.readObject();
                    messageList.add(message);
                    count++;
                }catch (UnknownHostException e) {
                    Log.e("C_UNKNOWN_HOST", "UnknownHostException: " + e.toString());
                }catch (EOFException e){
                    Log.e("C_EOF_EXCEPTION", "EOFException: " + e.toString());
                }
                catch (StreamCorruptedException e){
                    Log.e("C_STREAM_CORRUPT", "StreamCorruptedException: " + e.toString());
                }
                catch (SocketTimeoutException e){
                    Log.e("C_SOCKET_TIMEOUT", "SocketTimeoutException: " + e.toString());
                }
                catch (IOException e) {
                    Log.e("C_IO_EXCEPTION", "IOException: " + e.toString());
                }
                catch (Exception e){
                    Log.e("C_GEN_EXCEPTION","Exception: " + e.toString());
                }
            }

            /*
             *  This is one of the most important steps below. For ensuring TOTAL ORDERING, the
             *  messages should be delivered in the same orde across all clients. Hence, all
             *  clients must set a priority number based on consensus.
             *  Again, as always, the priority number is set to the maximum of proposed priority
             *  by all the clients to which the message was multicasted.
             */
            Message priority_consensus_msg=new Message(messageList.get(0));
            int consensus = seqNo;
            for(Message msg1: messageList) {
                if (msg1.getProposedPriority() > consensus) {
                    consensus = msg1.getProposedPriority();
                    priority_consensus_msg=msg1;
                }
            }
            seqNo=consensus;
            priority_consensus_msg.setConsensusPriority(consensus);

            /*
             * In the next part we have sockets of each client and the output streams.
             * We shall be multicasting a message to all the clients inclusing self, communicating
             * the final priority number which was deemed upon consensus.
             */
            for(int i=0;i<device_list.length;i++){
                try{
                    socket = sockets[i];
                    out_stream = out_streams[i];
                    out_stream.writeObject(priority_consensus_msg);
                    out_stream.flush();
                    socket.close();
                }catch (UnknownHostException e) {
                    Log.e("C_UNKNOWN_HOST", "UnknownHostException: " + e.toString());
                }catch (EOFException e){
                    Log.e("C_EOF_EXCEPTION", "EOFException: " + e.toString());
                }
                catch (StreamCorruptedException e){
                    Log.e("C_STREAM_CORRUPT", "StreamCorruptedException: " + e.toString());
                }
                catch (SocketTimeoutException e){
                    Log.e("C_SOCKET_TIMEOUT", "SocketTimeoutException: " + e.toString());
                }
                catch (IOException e) {
                    Log.e("C_IO_EXCEPTION", "IOException: " + e.toString());
                }
                catch (Exception e){
                    Log.e("C_GEN_EXCEPTION","Exception: " + e.toString());
                }
            }
            return null;
        }
    }

}