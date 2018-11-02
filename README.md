Branch|Status
---|---
master|[![CircleCI](https://circleci.com/gh/room-15/ChatSE/tree/master.svg?style=shield)](https://circleci.com/gh/room-15/ChatSE/tree/master)
develop|[![CircleCI](https://circleci.com/gh/room-15/ChatSE/tree/develop.svg?style=shield)](https://circleci.com/gh/room-15/ChatSE/tree/develop)

# ChatSE

Android app for Stack Exchange chat. Currently in development, see https://github.com/room-15/ChatSE/issues to view active issues. We'd love any community help.

# What the fkey is the fkey?

The fkey is the most important String inside this application. It is used to authenticate most calls (along with Cookie information). On any chat it is a unique ID that is passed to ensure that the user is the one making these calls. This key is persistent for each session. If you create a new tab while logged into the same account, your fkey will be the same. However, if you log in on a different machine, your fkey will be different there.

The fkey is a hidden HTML input at the end of the body with an id of `fkey`.

# How can you help?
  
I'm currently working on adding comments to this project, along with improving design and fixing bugs. If you'd like you help you can do a few things.

- Add features: This can be done by monitoring network calls in Chrome when you click on say, leave room, and then implementing that using something similar to the above code.
- Add comments: I'm sure I won't be able to get through all this code and add comments alone. Even just a few comments here and there help a lot.
- Identify bugs and create an issue. I'll try to get through all bugs asap.
- Code cleanup, there's a lot of code. Cleaning it up so others can help is tedious, but the most rewarding.
  
# Contributors
- [TristanWiley](https://github.com/TristanWiley) - I've revived this project to the best of my abilities and it's almost done
- [AnubianN00b](https://github.com/AnubianN00b) - Original creator of this project, ily <3
- [AdamMc331](https://github.com/AdamMc331) - My best friend and also helped work on the project.
- [Mauker](https://github.com/Mauker1) - r15 Dev Bro.
- [CptEric](https://github.com/tryadelion) - r15 Dev Bro.
  
~ Tristan Wiley
