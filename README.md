![GitHub issues](https://img.shields.io/github/issues-raw/Artuto/deluxechat-velocity-bridge) ![GitHub all releases](https://img.shields.io/github/downloads/Artuto/deluxechat-velocity-bridge/total)

# deluxechat-velocity-bridge

The original DeluxeChat bridge ported to Velocity.
This plugin will let you bridge server chats using DeluxeChat with Velocity.

## Requirements

- Your original DelxueChat plugin JAR.
- Java 11

## Download

Download from the [Releases page](https://github.com/Artuto/deluxechat-velocity-bridge/releases)

## Installation

1. Drop the plugin's jar into the plugins folder and start Velocity.
2. It will print an error on the console about the original DeluxeChat.jar not present, stop the proxy.
3. Go onto the plugins folder, search for this plugin's folder and drop the DeluxeChat.jar there

#### THE ORIGINAL DELUXECHAT JAR MUST BE NAMED `DeluxeChat.jar` OTHERWISE IT WILL NOT BE DETECTED

4. Start the proxy again
5. Profit!

### MAKE SURE YOU ENABLE BUNGEECORD/VELOCITY MESSAGING IN YOUR CONFIG:

![image](https://user-images.githubusercontent.com/9273973/147888645-3de26a9a-50db-482f-8559-ab741a1c0534.png)

![image](https://user-images.githubusercontent.com/9273973/147888656-8289c1ba-e8f6-4f2a-b092-d34e8ca6dfda.png)


## Compiling

If you want to compile this from source you'll need the following:

- JDK 11
- Maven
- Your original DelxueChat plugin JAR.

#### In order to resolve dependencies correct you must create a `libs` folder and place the `DeluxeChat.jar` file inside.
