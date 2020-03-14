# Genesis-M

Genetic-M is a utility for cell automata genetic algorithms simulation.

<img alt="The World Map" src="https://raw.githubusercontent.com/Abzac/Genesis/master/screenshot1.png">

# Prerequisites

You need to download and install the latest Java version from https://www.java.com/EN/download/
Then you can run the included `Genesis.jar` file by double-clicking on it, or by typing in your shell `java -jar Genesis.jar`.
You can quickly build the project inside IntelliJ Idea.

# How to start 

1. Scale the window according to your world size needs
2. Press "Generate map" button (once or a couple of times)
3. You may play with "Map scale" and "Sea level" sliders too.
4. Once you're happy with your map, click anywhere on the map to seed your first bot into the world!

Note: you better place it somewhere on the ground, not in the sea (or it'll probably die of starving).
Note2: sometimes, the first bot dies or refuses to multiply; in such cases, press the "Start/Stop" button to stop the simulation and then regenerate the map and make a new bot.

# Description

So, it's [cell automata](https://en.wikipedia.org/wiki/Cellular_automaton). 

We have the world in which each colored pixel represents a bot. A bot is a pretty smart thing with its program, willings, and hopes.
Each bot has its DNA; it has its family (a colony) and its foes; it needs a portion of food for its life, energy, and probably the place under the sun.

Each bot born, lives, and dies; also, it can reproduce, making a copy of itself, spending some food points on it; and finally, a bot can mutate and evolve. Some bots can even become vicious viruses for the others.

The most fitted bot colony survives, others die. So, on each iteration, they evolve and grow, trying to conquer their bots world. Their DNA is their program. DNA consist of 64 cells with one of 64 different commands, forming each bot behavior, attitude, and intelligence. Each step, every bot may become a little bit `smarter`, and learn some new things from the world around (or die).

Please see [Genetic algorithms wiki](https://en.wikipedia.org/wiki/Genetic_algorithm) for more information about the basics of genetic algorithms.

You can see the first version of the [program demo here](https://www.youtube.com/watch?v=PCx228KcOow) _(Russian)_

You can see more technical details [in this video](https://www.youtube.com/watch?v=jXa5IASmlkg) _(Russian too, sorry)_

The red bots tend to be more carnivorous, the green ones are mostly herbivorous producers consuming energy from the sun, and the blue ones tend to eat minerals.
But the color and bot specialties may vary through the specter.

Just wait for a few generations to spread around the map, and you'll see it!

# Contributions 

Original idea: [@foo52ru](https://www.youtube.com/channel/UCP1JsJgeNs86oqLGnjfGo9Q)
 
Forked from https://github.com/CyberBiology/Genesis

# Changes from the original version 

1. Threaded smooth graphics! Now you do not need to spend your precious single thread's processor cycles for graphics! So, you get more efficient and smooth drawing, and you also get faster world model updates! Wow!
2. Bots now have their genetic memories! Each bot starts with the memory data from some block of its DNA, and then it can update its knowledge about the world during its life.
3. Now you can see and trace the list of last bot commands for any bot, commands results, and arguments; on the right-side pane of the window. Discover what bot thinks of you!
4. Now you may see bots coordinates and color at the bottom of the window. Maybe it would help to catch one.
5. Now you can start the world with a first bot not only in the center of the screen but in any chosen point.
6. Some refactorings, fixes, and other minor improvements.
