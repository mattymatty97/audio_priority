Audio Engine Tweaks

this mod changes how Minecraft schedules sounds in order to prevent the sound pool to fill up
or at least mitigate the effects of a full sound pool

TLDR:
this mod fixes these errors ( log spam too ):
    [Render thread/WARN]: Failed to create new sound handle
    [Sound engine/WARN]: Maximum sound pool size 247 reached
and prevents the sound pool from overloading ( loss of all sounds in game until the pool frees up )

Details:
every tick sound are collected and categorized by sound type.
then the sounds are sorted by category ( lower number means higher priority ).
each category is also ordered by distance from the player.

depending on the fill level of the sound pool certain sound types are allowed to be player otherwise they are skipped.

if the sound pool gets full all successive sounds are skipped ( should never happen with the threshold rules ).

each tick there is a duplication check that will allow only a set number instances of the same sound to be played in the same coordinate ( 1 square block ),
excess are skipped.

sounds of MASTER and MUSIC Type are allowed to be played instantly ( if requested ) bypassing the priority queue and the duplication check
( this is because they can be requested by the Main Menu or the GUI while the internal server is frozen )
they will still follow the fill level threshold rules.
other Categories if requested to be played instantly, will instead be scheduled to the current tick

all the values are editable trough ModMenu config page or by manually editing the config file

Defaults:
MASTER	cat 0 and always allowed
VOICE	cat 0 and always allowed
PLAYERS	cat 6 and up to 95%
HOSTILE	cat 5 and up to 90%
BLOCKS	cat 4 and up to 80%
MUSIC	cat 3 and up to 70%
RECORDS	cat 3 and up to 70%
NEUTRAL	cat 2 and up to 60%
WEATHER	cat 1 and up to 50%
AMBIENT	cat 1 and up to 50%

maxDuplicatedSoundsByPos : 5
maxDuplicatedSoundByID   : 50

EDIT v1.2.4:
    since this version a new menu is available to directly mute any sound. ( or the relative config line that lists the muted sound by their ID )

    PLEASE if anybody good with UI sees this contact me, 
    having to scroll though the entire list of sounds is quite a pain to do, but I do not know how to make a proper UI.

EDIT v1.2.5:
    Added a search bar in the mute menu

    Inverted the default priorities so now you'll be able to hear sounds from all categories
