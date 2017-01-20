/**
 * Copyright 2011-17 Simon Andrews
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
 * This class provides a way to track a count via an object which 
 * doesn't need to be updated to a new object on every increment 
 * @author andrewss
 *
 */
public class NonThreadSafeIntCounter {

	private int value = 0;
	
	public void increment () {
		value++;
	}
	
	public void decrement () {
		value--;
	}
	
	public void incrementBy (int amount) {
		value += amount;
	}

	public void decrementBy (int amount) {
		value -= amount;
	}

	public int value () {
		return value;
	}
	
	
}
