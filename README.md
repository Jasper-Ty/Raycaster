# Raycaster
I wanted to try and learn how to do this, especially after watching a very cool YouTube video about the history of Ken Silverman's Build engine (by CuteFloor, watch it here: https://www.youtube.com/watch?v=l8tDBg4pfYk, I highly recommend it!)
I've been skipping off a lot of studying and homework while making this. To see that it's at the very least functional makes me very happy.

It's a JFrame with two components: The minimap, which stores info about the character and the level, and the camera, which stores info about the vertical columns being rendered. I used a Timer() to set the loop for the program, and in each loop movement & collision detection is done, the rays are casted, then the components are redrawn. A lot of my struggles with making this came with the odd drawing coordinate system used in Swing. The top-leftmost point is (0, 0) and you go down as you increase y. In any case, it works! Yay!
