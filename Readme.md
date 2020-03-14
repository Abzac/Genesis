# Genesis-M

Utility for cell automata genetic algorithms simulation.

<img alt="The World Map" src="https://raw.githubusercontent.com/Abzac/Genesis/master/screenshot1.png">

# How to start 

1. Scale the window according to your world size needs
2. Press "Generate map" button (once or a couple of times)
3. You may play with "Map scale" and "Sea level" slides too.
4. Once you're happy with your map, just click anywhere on the map to seed your first bot into the world!
Note: you better place it somewhere on the ground, not in the sea.
Note2: sometimes bot just dies or refuses to multiply, in such cases press "Start/Stop" button, and regenerate the map.

# Description

So, basically, it's [cell automata](https://en.wikipedia.org/wiki/Cellular_automaton). 

We have the world, in which each colored pixel represents a bot.
Each bot has its DNA, its own program, it has its family (a colony) and its foes, it needs a food, energy and probably the sun.

Each bot born, lives and dies; also it can reproduce, making a copy of itself, spending some food points; and finally a bot can mutate.
Some bots can even become vicious viruses for the others.

The most fitted bot family survives, others die. So, on each iteration they evolve and grow, trying to conquer their bots world.

Please see [Genetic alhorithms wiki](https://en.wikipedia.org/wiki/Genetic_algorithm) for more information about the basics of genetic algorithms.

You can see first version of the [program demo here](https://www.youtube.com/watch?v=PCx228KcOow) _(Russian)_

You can see more technical details [in this video](https://www.youtube.com/watch?v=jXa5IASmlkg) _(Russian too, sorry)_

The red bots tend to be more carnivorous, the green ones are mostly herbivorous producers consuming energy from the sun, and the blue ones tend to eat minerals.
But the color and bot specialities may vary through the spectre.

Just wait a few generations and you'll see it!

# Contributions 

Original idea: [@foo52ru](https://www.youtube.com/channel/UCP1JsJgeNs86oqLGnjfGo9Q)
 
Forked from https://github.com/CyberBiology/Genesis

# Changes from the original version 

1. Thread smooth graphics. Now you do not need to spend your only thread resources for graphics and world updates at the same time!
2. Bots now have genetic memory. They can update their knowledge about the world during their lifes, and each bot starts with the memory from their DNA.
3. Now you can see and trace the list of last bot commands, their result and arguments, on the right-side.
4. Now you may see bots coordinates and color in the bottom of the window.
5. Now you can start the world with a first bot not only in the center of the screen, but in some chosen point.
6. Some refactorings, fixes, and other minor improvements.
