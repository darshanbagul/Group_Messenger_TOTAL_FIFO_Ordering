# Group Messenger with TOTAL and FIFO ordering using modified ISIS algorithm

This project contains a submission for an assignment; requirement for the course CSE 586: Distributed Systems offered in Spring 2017 at State University of New York.

## Introduction

We implement advanced concepts in distributed systems that add ordering guarantees to a group messaging application. Specifically, we develop a **Group Messaging Android application with decentralized TOTAL and FIFO ordering guarantees.**

## Concepts
Before beginning the task, let us briefly understand some distributed system concepts that are necessary for devising a solution.
### Total Ordering

Total ordering implies that each process in the system delivers all messages in the same order. Here we don't care about any causal relationship of messages as long as every process follows a single order.

Let m0, m1, m2.....m8 represent messages sent by 3 members of the group chat (P1, P2, P3) in a specific order.
For example: -

    – P1: m0, m1, m2
    – P2: m3, m4, m5
    – P3: m6, m7, m8
One of the TOTAL ordering would be: - 

    – P1: m8, m1, m2, m4, m3, m5, m6, m0, m7
    – P2: m8, m1, m2, m4, m3, m5, m6, m0, m7
    – P3: m8, m1, m2, m4, m3, m5, m6, m0, m7

This implies each member (process) shall see the same ordering of messages in the group. This is intuitively easy to understand, or imagine the chaos in a group chat where everyone interprets information in a different manner with respect to the order that they receive messages in.

### FIFO Ordering

FIFO Ordering implies that the message delivery order at each process should preserve the message sending order from every process. But each process can deliver the message in a different order.

Following the notation from earlier we can explain as follows:

For example: -

    – P1: m0, m1, m2
    – P2: m3, m4, m5
    – P3: m6, m7, m8
One of the FIFO ordering would be: - 

    – P1: m0, m3, m6, m1, m2, m4, m7, m5, m8
    – P2: m3, m0, m1, m4, m6, m7, m5, m2, m8
    – P3: m6, m7, m8, m0, m1, m2, m3, m4, m5
    
Note that, as per FIFO Ordering for each process, m0 is received before m1, which is received before m2. Similarly for messages from other processes.

### TOTAL + FIFO Ordering

With an understanding of both the concepts individually, it is now easy to understand them together. Total and FIFO ordering implies that the message delivery order at each process should preserve the message sending order from every process and every process delivers all messages in the same order.

For example: -

For message sending order as below:

    – P1: m0, m1, m2
    – P2: m3, m4, m5
    – P3: m6, m7, m8

One of the TOTAL and FIFO ordering would be: - 

Message delivery order:

    – P1: m0, m3, m6, m1, m2, m4, m7, m5, m8
    – P2: m0, m3, m6, m1, m2, m4, m7, m5, m8
    – P3: m0, m3, m6, m1, m2, m4, m7, m5, m8

This is the absolute requirement for a non-chaotic group chat like Whatsapp or Facebook.

### ISIS Algorithm
Let us understand how ISIS algorithm works for guaranteeing Total + FIFO Ordering: 

  1. The multicast sender multicasts the message to all processes. 
  
  2. Recipients add the received message to a priority queue, tag the message undeliverable, and reply to the sender with a proposed priority (i.e., proposed sequence number). Further, this proposed priority is 1 more than the latest sequence number heard so far at the recipient, suffixed with the recipient's process ID. The priority queue is always sorted by priority.  
  
  3. The sender collects all responses from the recipients, calculates their maximum, and re-multicasts original message with this as the final priority for the message.
  
  4. On receiving this information, recipients mark the message as deliverable, reorder the priority queue, and deliver the set of lowest priority messages that are marked as deliverable.
  
Image shown below will help understand the process better:

![Image](https://github.com/darshanbagul/Group_Messenger_TOTAL_FIFO_Ordering/blob/master/images/ISIS_Algorithm_Working.gif)

**ISIS algorithm** developed at Cornell (Birman, 1993; Birman and Joseph, 1987a, 1987b; and Birman and Van Renesse, 1994) **provides Total ordered multicast delivery.** 

But we design and implement a **modified version of ISIS algorithm that guarantees both TOTAL and FIFO ordering** and provides a persistent Key-Value storage with ordering remaining intact even in case of application failures.

## Implementation
We implemented a decentralized algorithm which performs leader election(choosing the central node) locally. The messenger must be robust to handle app failures correctly as well.
The guidelines and rules for implementing the group messenger are available [here](https://docs.google.com/document/d/1xgXwZ6GYA152WT3K0B1MPP7F0mf0sPCPzfqr528pO5Y)

## Testing

We have been provided a [Grader](https://github.com/darshanbagul/Group_Messenger_TOTAL_FIFO_Ordering/tree/master/Grader) script, that tests our implementation rigorously by spawning multiple threads or multiple Android emulators. The testing is handled across two phases:
  
  **1. Testing without any failure**
    In this phase, all the messages should be delivered in a TOTAL-FIFO order. For each message, all the delivery sequence numbers should be the same across processes.
    
  **2. Testing with failures**
    In this phase, all the messages sent by live nodes should be delivered in a TOTAL-FIFO order. Due to a failure, the delivery sequence numbers might go out of sync if some nodes deliver messages from the failed node, while others do not. This is OK; the grader will only examine the TOTAL-FIFO ordering guarantees for the messages sent by live nodes.

### Running the Grader Script

  1. Load the Project in Android Studio and create the apk file.
  
  2. Download the Testing Program for your platform.
  
  3. Please also make sure that you have installed the app on all the AVDs.
  
  4. Before you run the program, please make sure that you are running five AVDs. The following command handles this:
       ```   
          python run_avd.py 5
       ```
  
  5. Also make sure that the Emulator Networking setup is done. Tis is handled by executing the following command
       ```
          python set_redir.py 10000
       ```
  
  6. Run the grader as illustrated:
       ```
          $ chmod +x < grader executable>
          
          $ ./< grader executable> apk file path
       ```
  
  7. Run the grader script with ‘-h’ argument for viewing help topics.

## Credits

This project comprises of scripts developed by Networked Systems Research Group at The State University of New York. I thank Prof. Steve Ko for teaching the course and encouraging practical implementations of important concepts in Large Scale Distributed Systems.

## References

   1. [Distributed Systems: Concepts and Design (5th Edition)](https://www.pearsonhighered.com/program/Coulouris-Distributed-Systems-Concepts-and-Design-5th-Edition/PGM85317.html) 

   2. Coursera MOOC - [Cloud Computing Concepts - University of Illinois at Urbana-Champaign](https://www.coursera.org/learn/cloud-computing) by Dr. Indranil Gupta
