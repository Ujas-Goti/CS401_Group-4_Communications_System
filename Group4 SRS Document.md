
**Group 4: Communications** 

##  *Software Requirements Specification* 

Revision History

| Date | Revision | Description | Author |
| :---- | :---- | :---- | :---- |
| mm/dd/yyyy | 1.0 | Initial Version | Your Name |


   
	Table of Contents

**1\.**	**Purpose	[4](#purpose)**

1.1.	Scope	[4](#scope)  
1.2.	Definitions, Acronyms, Abbreviations	[4](#definitions,-acronyms,-abbreviations)  
1.3.	References	[4](#references)  
1.4.	Overview	[4](#overview)

**2\.**	**Overall Description	[5](#overall-description)**

2.1.	Product Perspective	[5](#product-perspective)  
2.2.	Product Architecture	[5](#product-architecture)  
2.3.	Product Functionality/Features	[5](#product-functionality/features)  
2.4.	Constraints	[5](#constraints)  
2.5.	Assumptions and Dependencies	[5](#assumptions-and-dependencies)

**3\.**	**Specific Requirements	[6](#specific-requirements)**

3.1.	Functional Requirements	[6](#functional-requirements-\(describe-what-the-system-must-do\))  
3.2.	External Interface Requirements	[6](#external-interface-requirements)  
3.3.	Internal Interface Requirements	[7](#internal-interface-requirements)

**4\.**	**Non-Functional Requirements	[8](#non-functional-requirements)**

4.1.	Security and Privacy Requirements	[8](#security-and-privacy-requirements)  
4.2.	Environmental Requirements	[8](#environmental-requirements)  
4.3.	Performance Requirements	[8](#performance-requirements)


# 1. Purpose 
 
   This document outlines the requirements for a Communications System for a large organization. The goal is to provide both synchronous (real-time) and asynchronous (delayed) chat functionalities with support for private and group chats. IT administrators will be able to monitor all chat logs, while general users will have restricted access to their own conversations.


## 1.1 Scope

  This document will catalog the user, system, and hardware requirements for the Communication system. We will not, however, document how these requirements will be implemented.
   

## 1.2 Definitions, Acronyms, Abbreviations

     GUI – Graphical User Interface

     TCP/IP – Transmission Control Protocol / Internet Protocol

     IT User – Administrator with monitoring privileges

     General User – Employee with standard chat privileges

	 LAN - Local Access Network 


## 1.3 Refrences 

     Group project descriptions slide (08282025.odp) pg.12

     Developing Requirements lecture slides (developing_requirements.odp)

   	 SRS Template.docx 

	 UML Class Diagrams 

	 Use Case Diagram 

	 Sequence Diagram
   
   
   ## 1.4 Overview

This document catalogs the user, the system, and the hardware requirements for the Communication System, without detailing the implementation. The system will function as a Java desktop client-server application using TCP/IP sockets, providing a single GUI with features such as private and group chats, notifications, admin dashboards, and conversation logs, while supporting only text-based communication.


# 2. Overall Description


## 2.1 Product Perspective

This system will be a standalone Java application designed to enable secure real-time text information between users. The server application manages connections, message routing, and logging. The client application provides the user interface for messaging.


## 2.2 Product Architecture

The system will be organized into two major sub-systems: a client side application and a server side application. 
Server Application (back-end server): Routes all messages, manages authentication, manages client connections, manages chat history, and stores logs in text files
Client Application (front end): Provides the user with an interface (GUI) for sending/receiving text messages. IT admin clients can request server logs from text files.

## 2.3 Product Functionality/Features

The high-level features of the system are as follows. See section 3 of this document for more detailed requirements that address these features. 

Private and group chats 
Synchronous and asynchronous messaging 
Notifications for new messages 
Chat log access for IT admins 
Secure Log in with username and password 


## 2.4 Constraints

The application must run on any system with a Java runtime 
All users must be on the same organizational local network (LAN)
All chats must be text-based 
Conversation and Authentication can only be store in append-only text file


## 2.5 Assumptions and Dependecies

It is assumed that there is no limit to how many users are within an organization 
It is assumed that users are on the same organizational network.
It is assumed the server will be running before a client can connect.
It is assumed that each user will keep their unique ID private.
It is assumed that every user is using a Java-supported system to run the application.
The application is dependent on text files for the storage of chat logs and user information.





 




     


 



   
      


   
      

# 3. Specific Requirements

## 3.1 Functional Requirements

### 3.1.1 Common Requirements
- Users must log in with a unique username and secure password.  
- Users can send and receive private messages and group messages.  
- Users receive notifications for new messages.  
- Messages must be logged in text files (append-only).  

### 3.1.2 IT Admin Module Requirements
- IT admins shall be able to view chat logs in real-time sessions.  
- IT admins shall be able to search through the chat logs based on a unique userID and/or a unique groupID.  

### 3.1.3 Chat Management Module Requirements
- Chat management shall be able to send text messages instantly in a group chat or a private chat.  
- Chat management shall generate a unique chat ID for every chat that gets created.  
- Chat management shall load old chat logs and update them with new messages.  

### 3.1.4 Logger Module Requirements
- The logger shall include a timestamp for all recorded logs.  
- The logger shall record when chats are created (private or group) and list participants (regular users or IT admins).  
- The logger shall record the sender of a text message and the intended recipient.  

### 3.1.5 Authentication Module Requirements
- A user must be authenticated before being given access to any features.  
- A user’s authorized actions are based on their unique ID.  

### 3.1.6 Notifications Module Requirements
- The Notifications module shall notify the user when they receive a message if it is delivered asynchronously.  

### 3.1.7 Network/Communication Module Requirements
- The server must be scalable to support increasing numbers of concurrent users and data without performance degradation.  

### 3.1.8 Connection Manager Module Requirements
- The module shall maintain a list of active connections.  
- The module shall continuously update the list of active connections when a new user connects or disconnects.  

---

### 3.2 External Interface Requirements
- The system shall provide a login interface for a regular user.  
- The system shall provide a login interface for an IT user.  
- The system shall provide an interface for users to send and receive messages.  
- The system shall provide an interface for IT Admins with tools to view other users’ chat logs.  

---

### 3.3 Internal Interface Requirements
- The Chat Management module shall structure messages with metadata, and the Network module shall transmit them reliably via TCP sockets.  
- Chat Management shall send the message to be logged by the Logger.  
- IT Admin Tools shall request a specific log from the Logger. The Logger shall fetch the requested log and return it to IT Admin Tools.  
- The Chat Management module shall notify the Notifications module when a new message has been received asynchronously.  
- The Authentication module shall validate login credentials.  
  - If login succeeds: the Chat Management module shall create a new chat session for the user.  
  - If login fails: the user shall be denied access.  
  

# 4. Non-Functional Requirements  

## 4.1 Security and Privacy Requirements  
- The system shall maintain integrity ensuring logs are append-only and tamper-proof for preserving log reliability.  
- The system shall enforce role-based access control with 100% reliability ensuring only users with IT Admin can access chats.  
- User role and ID storage shall ensure data integrity for authorized administrators.  

## 4.2 Environmental Requirements (ex: hardware, software, network, physical factors)  
- Client machine should be able to run Java SE 8 or later.  
- Client requires organizational LAN connectivity.  

## 4.3 Performance Requirements  
- The system shall achieve reliable message delivery ensuring that all messages are delivered to the intended recipient.  
- The system shall guarantee the integrity of messages, ensuring the delivery of a complete text message without truncation.  
- The system shall maintain persistent chat history per user, ensuring the data availability.  
- The server hardware shall support horizontal scalability to support an increasing number of users while maintaining acceptable performance levels.  


