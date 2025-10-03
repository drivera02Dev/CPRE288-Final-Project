/*
 * music.c
 *
 *  Created on: May 5, 2025
 *      Author: andreyd
 */


#include "music.h"


void load_song(int song_index, int num_notes, unsigned char *notes, unsigned char *duration) {
    oi_loadSong(song_index, num_notes, notes, duration);
}


void play_song(int index) {
    oi_play_song(index);
}
