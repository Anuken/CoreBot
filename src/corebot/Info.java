package corebot;

public enum Info{
    links("Relevant Links",
    "[Github](https://github.com/Anuken/Mindustry/)\n" +
    "[Trello](https://trello.com/b/aE2tcUwF)\n" +
    "[PC/Web/Android Versions](https://anuke.itch.io/mindustry)\n" +
    "[Itch.io App / Updater](https://itch.io/app)\n" +
    "[iOS version](https://itunes.apple.com/us/app/mindustry/id1385258906?mt=8&ign-mpt=uo%3D8)\n" +
    "[Google Play Listing](https://play.google.com/store/apps/details?id=mindustry)\n" +
    "[TestFlight Link](https://testflight.apple.com/join/79Azm1hZ)\n" +
    "[3.5 Wiki](http://mindustry.wikia.com/wiki/Mindustry_Wiki)\n"),
    beta("iOS/Android Beta",
    "To join the Android Beta in Google Play Store, just scroll to the bottom of the page and tap 'join beta'.\n" +
    "Betas can take some time to become available after release. Be patient.\n" +
    "[Direct Link](https://play.google.com/apps/testing/mindustry)\n\n" +
    "To join the iOS beta, click this [TestFlight Link](https://testflight.apple.com/join/79Azm1hZ), then install the Apple TestFlight app to play Mindustry."),
    rules("Rules",
    "**1.** Don't be rude. This should be obvious. No racism/sexism/etc.\n" +
    "**2.** No spamming or advertising. Discord links are automatically deleted by the bot; if you want to invite someone to a server, do so in PM.\n" +
    "**3.** No NSFW content or politics. This includes NSFW conversations. Take it elsewhere.\n" +
    "**4.** Keep content to the aproppriate text channels.\n" +
    "**5.** Please keep memes to a minimum; don't dump memes. Mindustry-related memes are fine.\n" +
    "**6.** Please do not post invite links to this server in public places without context.\n" +
    "**7.** Do not ask for roles. If I need a moderator and I think you fit the position, I will ask you personally.\n" +
    "**8.** Do not impersonate other members or intentionally edit your messages to mislead others.\n" +
    "**9.** Do not cross-post the same message to multiple channels.\n" +
    "**11.** Breaking any of these rules in a non-serious way may result in a warning. 3 warnings is a ban.\n" +
    "*If I don't like your behavior, you're out. Obey the spirit, not the word.*"),
    pings("Ping and PM Policy",
    "*Please do not PM or ping me (Anuke) unless it is something important.*\n\n" +
    "'Important' includes things like major unreported game server issues or exploits, " +
    "problems with the Discord server (e.g. spammers), or Github pull request related issues." +
    "\n" +
    "Do *not* PM or ping me when posting normal suggestions or bugs reports. If it's relevant, I'll read it.\nIf you have a question about the game, *ask in #mindustry or #help.*\n" +
    "\n" +
    "I will not respond to PMs with non-personal game-related questions or random messages such as 'hey'. And no, video calling me multiple times in PM doesn't get my attention, it just gets you blocked. *(Believe me, people have tried.)*\n" +
    "*I also do not accept random friend requests from people I don't know.*");
    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
