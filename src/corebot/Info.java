package corebot;

public enum Info{
    links("Relevant Links",
    """
    [Github](https://github.com/Anuken/Mindustry/)
    [Trello](https://trello.com/b/aE2tcUwF)
    [Steam version](https://store.steampowered.com/app/1127400/Mindustry/)
    [PC/Web/Android Versions](https://anuke.itch.io/mindustry)
    [iOS version](https://itunes.apple.com/us/app/mindustry/id1385258906?mt=8&ign-mpt=uo%3D8)
    [Google Play Listing](https://play.google.com/store/apps/details?id=mindustry)
    [TestFlight Link](https://testflight.apple.com/join/79Azm1hZ)
    [Unofficial Matrix](https://matrix.to/#/!bjBcJCYyWfNVdAAWZt:matrix.org)
    """),
    beta("iOS Beta",
    """
    To join the iOS beta, click this [TestFlight Link](https://testflight.apple.com/join/79Azm1hZ), then install the Apple TestFlight app to play Mindustry.
    
    There is currently no beta available on Google Play. Download the itch.io version.
    """),
    rules("Rules",
    """
    **1.** Don't be rude. This should be obvious. No racism/sexism/etc.
    **2.** No spamming or advertising.
    **3.** No NSFW, sensitive or political content. This includes NSFW conversations. Take it elsewhere.
    **4.** Keep content to the appropriate text channels.
    **5.** Please do not post invite links to this server in public places without context.
    **6.** Do not ask for roles. If I need a moderator and I think you fit the position, I will ask you personally.
    **7.** Do not impersonate other members or intentionally edit your messages to mislead others.
    **8.** Do not cross-post the same message to multiple channels.
    **9.** Do not advertise in DMs or send unsolicited messages to other users. Report violations of this sort to moderators immediately.
    **10.** Ban evasion and alternate accounts are not allowed. Alts posting any content on the server will be banned immediately.
    *If I don't like your behavior, you're out. Obey the spirit, not the word.*
    """),
    pings("Ping and PM Policy",
    """
    *Please do not PM or ping me (Anuke) unless it is something important.*
        
    'Important' includes things like major unreported game server issues or exploits, problems with the Discord server (e.g. spammers), or Github pull request related issues.
    Do *not* PM or ping me when posting normal suggestions or bugs reports. If it's relevant, I'll read it.
    If you have a question about the game, *ask in #mindustry or #help.*
        
    I will not respond to PMs with non-personal game-related questions or random messages such as 'hey'. And no, video calling me multiple times in PM doesn't get my attention, it just gets you blocked.
    *I also do not accept random friend requests from people I don't know.*
    """);
    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
