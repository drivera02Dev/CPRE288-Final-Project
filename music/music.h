/*
 * music.h
 *
 *  Created on: May 5, 2025
 *      Author: andreyd
 */

#ifndef MUSIC_H_
#define MUSIC_H_

#include "open_interface.h"


void load_song(int song_index, int num_notes, unsigned char *notes, unsigned char *duration);

void play_song(int index);




#endif /* MUSIC_H_ */
