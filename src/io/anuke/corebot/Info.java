package io.anuke.corebot;

public enum Info {
    links("Relevant Links",
            "[Github](https://github.com/Anuken/Mindustry/)\n" +
            "[Latest Builds](https://github.com/Anuken/Mindustry/wiki)\n" +
            "[Plans, Suggestions](https://github.com/Anuken/Mindustry/blob/master/TODO.md)\n" +
            "[4.0 Info](https://github.com/Anuken/Mindustry/issues/87)\n" +
            "[Downloads, Web Version](https://anuke.itch.io/mindustry)\n" +
            "[Itch.io App / Updater](https://itch.io/app)\n" +
            "[Google Play Listing](https://play.google.com/store/apps/details?id=io.anuke.mindustry)\n" +
            "[Wiki](http://mindustry.wikia.com/wiki/Mindustry_Wiki)\n"),
    beta("Android Beta",
            "To join the Android Beta in Google Play Store, just scroll to the bottom of the page and tap 'join beta'.\n" +
            "Betas can take some time to become available after release. Be patient.\n" +
            "[Direct Link](https://play.google.com/apps/testing/io.anuke.mindustry)"),
    rules("Rules",
            "**1.** Don't be rude. This should be obvious. No racism/sexism/etc.\n" +
            "**2.** No spamming or advertising.\n" +
            "**3.** No NSFW content or politics. This includes NSFW conversations. Take it elsewhere.\n" +
            "**4.** Keep content to the aproppriate text channels.\n" +
            "**5.** Please keep memes to a minimum.\n" +
            "**6.** Please do not post invite links to this server in public places.\n" +
            "**7.** Do not beg for roles.\n" +
            "**8.** Do not impersonate other members or intentionally edit your messages to mislead others.\n" +
            "*If I don't like your behavior, you're out. Obey the spirit, not the word.*"),
    multiplayer("Multiplayer",
            "Multiplayer in Mindustry works just as it does in many other sandbox games.\n" + 
            "To host a server, press *'host'* in the menu while playing.\n" +
            "Your server's IP can be found by googling `my ip`.\n" +
            "To join a server, enter their IP in the 'add server' box and click the server.\n" + 
            "Both over-the-internet and LAN play is supported. For someone outside your network to connect to your games, **port forwarding is required.**\n" +
            "Multiplayer is cross-platform, although **the web version does not support hosting.**\n\n" + 
            "*For more info on how to port forward, check the pinned messages in #servers.*\n"),
    server("Dedicated Server",
            "To run the dedicated server .JAR file, you need to use the command prompt or terminal. **Java is required**.\n\n" + 
            "Navigate to the file location and type `java -jar <server-file-name>.jar`, which should start the server.\n" + 
            "Note that the server does not automatically start hosting. Type `host <map> <gamemode>` to open the server.\n" + 
            "Type `help` for a list of all commands.\n"),
    suggested("Suggested Ideas",
        "**The following ideas have been suggested many times before, and will most likely not be added to the game.**\n\n" +
        "- Texture packs\n" +
        "- Online player profiles\n" +
        "- Player mech on Android\n" +
        "- Modding support\n" +
        "- Game speed increase (fast forward)\n" +
        "- Liquid teleporter\n" +
        "- More teleporter colors/any system that gives it more frequencies (numbers)\n" +
        "- Power wires or conduits\n" +
        "- Map browser\n" +
        "- Server brower\n" +
        "- Underground map layer\n" +
        "- Tech tree, research");

    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
