
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

1. # **Purpose** {#purpose}
 
   This SRS document outlines and defines the requirements of a communication system for a very large organization. The goal is to provide both synchronous(real-time) and asynchronous(delayed) chat functionalities with the support for private and group chats. IT administrators will have access to monitor all chat logs, while general users will have restricted access to their own conversations.

   1.1 ## **Scope** {#scope}

   This document will catalog the user, the system, and the hardware requirements for the communication system. It will not, however, document how these requirements will be implemented.
   

   1.2 ## **Definitions, Acronyms, Abbreviations** {#definitions,-acronyms,-abbreviations}

     GUI – Graphical User Interface

     TCP/IP – Transmission Control Protocol / Internet Protocol

     IT User – Administrator with monitoring privileges

     General User – Employee with standard chat privileges


   1.3. ## **References** {#references}

     Group project descriptions slide (08282025.odp) pg.12

     Developing Requirements lecture slides (developing_requirements.odp)

   	 SRS Template.docx 
   
   

      
1.1 
      The system will operate as a Java desktop application using TCP/IP sockets for communication.
	  Maintain conversation logs in text files with timestamos, senders unique ID, and chat type (private/group) 
	  Provide a single GUI interface for general users and for IT administrators.
	  Support private one-to-one chats and group conversations.
	  Enable notifications for new messages.
	  Enforce text-only chats
	  Function as client-server  with a centralized server handling connections.

      

Constraints:

* No external frameworks, libraries, databases, or HTML/web components will be allowed.
* All logs must be stored in **local files** on the server.  
* All users must be on the same local server.  
* All chats must be text-based.


 




     

  4. ## **Overview** {#overview}

     This document catalogs the user, the system, and the hardware requirements for the Communication System, without detailing the implementation. The system will function as a Java desktop client-server application using TCP/IP sockets, providing a single GUI with features such as private and group chats, notifications, admin dashboards, and conversation logs, while supporting only text-based communication.
 

2. # **Overall Description** {#overall-description}

   1. ## **Product Perspective** {#product-perspective}

      This system will be a **standalone Java application**. The **server application** manages connections, message routing, and logging. The **client application** provides the user interface for messaging.

   2. ## **Product Architecture** {#product-architecture}

      The system will be organized into \_\_\_ major modules: the IT module, the \_\_\_ module, and the \_\_\_\_\_ module.

      Note: System architecture should follow standard OO design practices.

      The system will be organized into two major modules: a client side application and a server side application. 

      **Server Application**: Routes all messages, manages client connections, and **stores logs in text files** (with format: `[timestamp] [user] [private/group] [message]`).

      **Client Application**: GUI for sending/receiving messages. IT admin clients can request server logs (read from text files).

      

      **Logger Module:** 

   3. ## **Product Functionality/Features** {#product-functionality/features}

      The high-level features of the system are as follows (see section 3 of this document for more detailed requirements that address these features):

      Private and group chat

      Synchronous and asynchronous messaging

      Notifications for new messages

      Conversation logging (text only) 

      Separate GUIs for IT and general users

      

   4. ## **Constraints** {#constraints}

      List appropriate constraints.

      Constraint example: Since users may use any web browser to access the system, no browser-specific code is to be used in the system. 

      Since users will access the application through a number of different mobile phones there will be no platform- specific code. 

      

      Only **text messages** are supported.

      No external libraries, frameworks, or databases may be used.

      Application must run on any system with a Java runtime.

      

   5. ## **Assumptions and Dependencies** {#assumptions-and-dependencies}

      List appropriate assumptions

      Assumption Example: It is assumed that the maximum number of users at a given time is 15,000.

      It is assumed that there is no limit to how many users can access the application   

      

      Users are on the same organizational network.

      Server must be running before clients can connect.

      Each user has a unique ID (username).

      

3. # **Specific Requirements** {#specific-requirements}

   1. ## **Functional Requirements (Describe What the system must do)** {#functional-requirements-(describe-what-the-system-must-do)}

      1. ### **Common Requirements:**

      Provide requirements that apply to all components as appropriate. 

      Example:

      3.1.1.1 Users should be allowed to log in using their issued id and pin, both of which are alphanumeric strings between 6 and 20 characters in length.

- The system shall allow users to log in using a unique email and password combination  before accessing the chat system.  
- The system shall allow users to send and receive messages in real time  

  3.1.1.2 The system should provide HTML-based help pages on each screen that describe the purpose of each function within the system. 

  2. ### **IT Module Requirements:**

     

     Provide module specific requirements as appropriate. 

     Example:

     3.1.2.1 Users should be allowed to log in using their issued id and pin, both of which are alphanumeric strings between 6 and 20 characters in length. 

     3. ### **General User Module Requirements:**

     Provide module specific requirements as appropriate. 	

     Example:

     3.1.2.1 Users should be allowed to log in using their issued id and pin, both of which are alphanumeric strings between 6 and 20 characters in length.

     4. ### **Logger Module Requirements:**

     Provide module specific requirements as appropriate. 	

     Example:

     3.1.2.1 Users should be allowed to log in using their issued id and pin, both of which are alphanumeric strings between 6 and 20 characters in length.

     

  2. ## **External Interface Requirements** {#external-interface-requirements}

     Provide module specific requirements as appropriate. 

     Example:

     3.2.1 The system must provide an interface to the University billing system administered by the Bursar’s office so that students can be automatically billed for the courses in which they have enrolled. The interface is to be in a comma-separated text file containing the following fields: student id, course id, term id, action. Where “action” is whether the student has added or dropped the course. The file will be exported nightly and will contain new transactions only. 

  3. ## **Internal Interface Requirements** {#internal-interface-requirements}

     Provide module specific requirements as appropriate. 

     Example:

     3.3.1 The system must process a data-feed from the grading system such that student grades are stored along with the historical student course enrolments. Data feed will be in the form of a comma-separated interface file that is exported from the grading system nightly.

     3.3.2 The system must process a data-feed from the University billing system that contains new student records. The feed will be in the form of a comma-separated text file and will be exported from the billing system nightly with new student records. The fields included in the file are student name, student id, and student pin number.  

4. # **Non-Functional Requirements** {#non-functional-requirements}

   1. ## **Security and Privacy Requirements** {#security-and-privacy-requirements}

   Example:

   4.1.1 The System must encrypt data being transmitted over the Internet. 

   

   2. ## **Environmental Requirements** {#environmental-requirements}

      Example:

      4.2.1 System cannot require that any software other than a web browser be installed on user computers. 

      4.2.2 System must make use of the University’s existing Oracle 9i implementation for its database. 

      4.2.3 System must be deployed on existing Linux-based server infrastructure. 

   3. ## **Performance Requirements** {#performance-requirements}

      Example:

      4.3.1 System must render all UI pages in no more than 9 seconds for dynamic pages. Static pages (HTML-only) must be rendered in less than 3 seconds. 

- The system must deliver a message in under 10 seconds in normal network conditions   
-    

### 

