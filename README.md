# ChatSE

Android app for Stack Exchange chat. Currently in development, see https://github.com/room-15/ChatSE/issues to view active issues. We'd love any community help.

# Application Flow

The app is loaded to the LoginActivity, if the user has not logged in yet the app asks for a email and password to authenticate all calls. This then makes a call to the appropriate logins (for Stackoverflow and Stackexchange) then saves those cookies for further use.

If the user has already logged in the application moves on to the next step. It loads the ChatActivity, this loads in a ChatFragment, as of writing this it loads Room 15 (Android). The app makes network calls to get the rooms the user is in and populates the NavigationDrawer (calls https://chat.stackoverflow.com/users/thumbs/{user_chat_id] and likewise for SE if applicable).

The app also identifies what users are currently in the chat but viewing all user_ids mentioned in past chats. This populates the NavigationDrawer on the right. Any new users adds to the drawer, and any users that leave removes from the drawer.

The ChatFragment is loaded into the ChatActivity and contains a RecyclerView where each item is a new chat. Each chat is handled by a few services and listeners. Some of the most important ones are as follows.

  - MessageEventPresentor, where messages are added to an ArrayList of MessageEvents. This implements EventPresentor which overrides getEventsList and getUsersList which get the list of messages and users respectively. Inside this class, events are handled based on the event_type that Stackexchange assigns it. For example, the event_type of 1 is a new message, and we simply add a new MessageEvent to the messages list. 2 is an edit and it replaces the old message with the new, etc.
  
  - MessageAdapter is where messages in the main RecyclerView are handled. In this adapter, messages are displayed with the time, username, and content. Along with any appropriate stars and edit messages. On the long click of messages, the option to star, edit, and delete are displayed when the appropriate permissions are available for them.
  
New calls are authenticated using OkHttp so that the appropriate cookies are sent with it. A sample call with be something like

```
doAsync {
    val client = ClientManager.client

    val soRequestBody = FormEncodingBuilder()
            .add("fkey", fkey)
            .add("quiet", "true")
            .build()
    val soChatPageRequest = Request.Builder()
            .url(site + "/chats/leave/" + roomID)
            .post(soRequestBody)
            .build()
    client.newCall(soChatPageRequest).execute()
}
```
This call is used to leave a room, where the request body contains parameters, including the fkey (which will be explained in a moment), and other necessary parameters) Then the request is built with the url and body combined and executed.

# What the fkey is the fkey?

The fkey is the most important String inside this application. It is used to authenticate most calls (along with Cookie information). On any chat it is a unique ID that is passed to ensure that the user is the one making these calls.

# How can you help?
  
 I'm currently working on adding comments to this project, along with improving design and fixing bugs. If you'd like you help you can do a few things.
 
  - Add features: This can be done by monitoring network calls in Chrome when you click on say, leave room, and then implementing that using something similar to the above code.
  - Add comments: I'm sure I won't be able to get through all this code and add comments alone. Even just a few comments here and there help a lot.
  - Identify bugs and create an issue. I'll try to get through all bugs asap.
  - Code cleanup, there's a lot of code. Cleaning it up so others can help is tedius, but the most rewarding.
  
# Contributers
  - [TristanWiley](https://github.com/TristanWiley) - I've revived this project to the best of my abilities and it's almost done
  - [AnubianN00b](https://github.com/AnubianN00b) - Original creator of this project, ily <3
  - [AdamMc331](https://github.com/AdamMc331) - My best friend and also helped work on the project.
  
  
~ Tristan Wiley
      
