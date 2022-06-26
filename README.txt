every tick sound are collected and sorted by SoundCategory:
MASTER	cat 0
VOICE	cat 0
PLAYERS	cat 1
HOSTILE	cat 2
BLOCKS	cat 3
MUSIC	cat 4
RECORDS	cat 4
NEUTRAL	cat 5
WEATHER	cat 6
AMBIENT	cat 6

each category is also ordered by distance from the player.

depending on the fill level of the sound pool certain categories are allowed to be player otherwise they are skipped:
cat 0	always allowed
cat 1	up to 95%
cat 2	up to 90%
cat 3	up to 80%
cat 4	up to 70%
cat 5	up to 60%
cat 6	up to 50%

if the sound pool gets full all successive sounds are skipped ( should never happen with the threshold rules )

each tick there is a duplication check that will allow only 5 instances of the same sound to be played in the same block, excess are skipped

sounds in MASTER and MUSIC Category are allowed to be played instantly ( if requested ) bypassing the priority queue and the duplication check ( this is beacuse they can be requested by the Main Menu or the GUI while the internal server is frozen )
they will still follow the fill level threshold rules.
other Categories if requested to be played instantly, will instead be scheduled to the current tick


