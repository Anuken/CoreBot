package io.anuke.corebot;

public enum Info {
    links("Relevant Links",
            "[Github](https://github.com/Anuken/Mindustry/)\n" +
            "[Latest Builds](https://github.com/Anuken/Mindustry/wiki)\n" +
            "[Plans, Suggestions](https://github.com/Anuken/Mindustry/blob/master/TODO.md)\n" +
            "[4.0 Info](https://github.com/Anuken/Mindustry/issues/87)\n" +
            "[Downloads, Web Version](https://anuke.itch.io/mindustry)\n" +
            "[Google Play Listing](https://play.google.com/store/apps/details?id=io.anuke.mindustry)\n" +
            "[Wiki](http://mindustry.wikia.com/wiki/Mindustry_Wiki)\n"
    ),
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
            "*If I don't like your behavior, you're out. Obey the spirit, not the word.*");

    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
