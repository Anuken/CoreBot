package corebot;

public enum Info{
    links("Relevant Links",
    """
    [Source Code on Github](https://github.com/Anuken/Mindustry/)
    [Suggestion Form](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose)
    [Bug Report Form](https://github.com/Anuken/Mindustry/issues/new/choose)
    [Trello](https://trello.com/b/aE2tcUwF)
    [Steam Version](https://store.steampowered.com/app/1127400/Mindustry/)
    [Android APKs and itch.io version](https://anuke.itch.io/mindustry)
    [iOS version](https://itunes.apple.com/us/app/mindustry/id1385258906?mt=8&ign-mpt=uo%3D8)
    [Google Play Listing](https://play.google.com/store/apps/details?id=mindustry)
    [TestFlight Link](https://testflight.apple.com/join/79Azm1hZ)
    [Mindustry Subreddit](https://www.reddit.com/r/mindustry)
    [Unofficial Matrix Space](https://matrix.to/#/#mindustry-space:matrix.org)
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
    
    **11.** Please do not PM me (Anuke) unless you are reporting an exploit, a significant problem with the Discord server, or need to discuss something relating to Github PRs. If you need help with the game, *ask in #help. or #mindustry*. Do *not* PM me suggestions - use the suggestions form.
    
    *If I don't like your behavior, you're out. Obey the spirit, not the word.*
    """);
    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
