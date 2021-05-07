package xyz.artuto.deluxechat.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.clip.deluxechat.bungee.PMChan;
import me.clip.deluxechat.messaging.PrivateMessageType;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from;

@SuppressWarnings("UnstableApiUsage")
@Plugin(id = "deluxechat-velocity", name = "DeluxeChat Velocity Bridge", version = "1.0",
        authors = "Artuto")
public class VelocityBridge
{
    private final Logger logger;
    private final Path dataFolder;
    private final ProxyServer server;

    private boolean init = false;

    @Inject
    public VelocityBridge(Logger logger, @DataDirectory Path dataFolder, ProxyServer server)
    {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.server = server;

        try {Files.createDirectories(dataFolder);}
        catch(Exception e) {throw new RuntimeException("Failed to create plugin directory!", e);}

        Path deluxeChatPlugin = dataFolder.resolve("DeluxeChat.jar");
        if(!(deluxeChatPlugin.toFile().exists()))
        {
            logger.error("Could not find DeluxeChat.jar in the plugin's directory.");
            logger.warn("Please make sure the original DeluxeChat jar is present.");
            logger.warn("After placing it there please restart the server.");
            return;
        }

        this.init = true;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        if(!(init))
            return;

        Path deluxeChatPlugin = dataFolder.resolve("DeluxeChat.jar");
        try {server.getPluginManager().addToClasspath(this, deluxeChatPlugin);}
        catch(Exception e) {throw new RuntimeException("Failed to load DeluxeChat into the classpath:", e);}
        logger.info("Loaded DeluxeChat into the classpath!");

        this.CHAT = from(PMChan.CHAT.getChannelName());
        this.PM   = from(PMChan.PM.getChannelName());
        this.SPY  = from(PMChan.SPY.getChannelName());

        logger.info("Registering channels...");
        server.getChannelRegistrar().register(CHAT);
        server.getChannelRegistrar().register(PM);
        server.getChannelRegistrar().register(SPY);
        logger.info("DeluxeChat Bridge loaded!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event)
    {
        if(!(init))
            return;

        server.getChannelRegistrar().unregister(CHAT);
        server.getChannelRegistrar().unregister(PM);
        server.getChannelRegistrar().unregister(SPY);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event)
    {
        var identifier = (MinecraftChannelIdentifier) event.getIdentifier();
        if(!(identifier.getNamespace().equals("dchat")))
            return;

        if(!(event.getSource() instanceof ServerConnection))
            return;

        switch(identifier.getName())
        {
            case "chat":
                ServerConnection source = (ServerConnection) event.getSource();
                ServerInfo sourceInfo = source.getServerInfo();
                for(RegisteredServer server : server.getAllServers())
                {
                    int players = server.getPlayersConnected().size();
                    ServerInfo serverInfo = server.getServerInfo();
                    if(!(serverInfo.getAddress().equals(sourceInfo.getAddress())) && players > 0)
                        server.sendPluginMessage(CHAT, event.getData());
                }
                break;
            case "pm":
                ServerConnection serverSource = (ServerConnection) event.getSource();
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                PrivateMessageType type = PrivateMessageType.fromName(in.readUTF());
                if(!(type == PrivateMessageType.MESSAGE_SEND))
                    return;

                Player sender = server.getPlayer(in.readUTF()).orElse(null);
                String recipient = in.readUTF();
                Player recipientPlayer = server.getPlayer(recipient).orElse(null);
                String format = in.readUTF();
                String jsonMessage = in.readUTF();
                String rawMessage = in.readUTF();
                if(sender == null)
                    return;

                if(recipientPlayer == null)
                {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF(PrivateMessageType.MESSAGE_SENT_FAIL.getType());
                    out.writeUTF(sender.getUsername());
                    out.writeUTF(recipient);
                    out.writeUTF(format);
                    out.writeUTF(jsonMessage);
                    out.writeUTF(rawMessage);
                    serverSource.sendPluginMessage(PM, out.toByteArray());
                    return;
                }

                // send message to the recipient
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(PrivateMessageType.MESSAGE_TO_RECIPIENT.getType());
                out.writeUTF(sender.getUsername());
                out.writeUTF(recipientPlayer.getUsername());
                out.writeUTF(format);
                out.writeUTF(jsonMessage);
                out.writeUTF(rawMessage);
                recipientPlayer.getCurrentServer().get().sendPluginMessage(PM, out.toByteArray());
                // send success message to the sender
                ByteArrayDataOutput senderOut = ByteStreams.newDataOutput();
                senderOut.writeUTF(PrivateMessageType.MESSAGE_SENT_SUCCESS.getType());
                senderOut.writeUTF(sender.getUsername());
                senderOut.writeUTF(recipientPlayer.getUsername());
                senderOut.writeUTF(format);
                senderOut.writeUTF(jsonMessage);
                senderOut.writeUTF(rawMessage);
                sender.getCurrentServer().get().sendPluginMessage(PM, senderOut.toByteArray());
                // send social spy message
                ByteArrayDataOutput socialspy = ByteStreams.newDataOutput();
                socialspy.writeUTF(sender.getUsername());
                socialspy.writeUTF(recipientPlayer.getUsername());
                socialspy.writeUTF(rawMessage);
                byte[] data = socialspy.toByteArray();

                InetSocketAddress senderAddr = serverSource.getServerInfo().getAddress();
                for(RegisteredServer registeredServer : this.server.getAllServers())
                {
                    int players = registeredServer.getPlayersConnected().size();
                    InetSocketAddress addr = registeredServer.getServerInfo().getAddress();
                    if(!(addr.equals(senderAddr)) && players > 0)
                        registeredServer.sendPluginMessage(SPY, data);
                }
        }
    }

    private MinecraftChannelIdentifier CHAT;
    private MinecraftChannelIdentifier PM;
    private MinecraftChannelIdentifier SPY;
}
