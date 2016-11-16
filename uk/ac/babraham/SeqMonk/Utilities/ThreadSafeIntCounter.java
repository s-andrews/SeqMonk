/**
 * Copyright 2011-15 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Utilities;
/**
 * This class provides a thread safe implementation of a counter
 * where the increment and decrement methods can be called from
 * any number of threads with no concern that the values will 
 * clash or updates be lost.
 * 
 * @author andrewss
 *
 */
public class ThreadSafeIntCounter {

	private int value = 0;
	
	public synchronized void increment () {
		value++;
	}
	
	public synchronized void decrement () {
		value--;
	}
	
	public synchronized void incrementBy (int amount) {
		value += amount;
	}

	public synchronized void decrementBy (int amount) {
		value -= amount;
	}

	public int value () {
		return value;
	}
	
	
}
