/**
 * Copyright Copyright 2017-19 Simon Andrews
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

package uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;

public class CountedException implements Comparable<CountedException>{

	
	private Exception exception;
	private int count;
	
	
	public CountedException (Exception exception) {
		this.exception = exception;
		count = 1;
	}
	
	public void increment () {
		++count;
	}
	
	public Exception exception () {
		return exception;
	}
	
	public int count () {
		return count;
	}
	
	public String toString() {
		return "["+count+" times] "+exception.getMessage();
	}

	@Override
	public int compareTo(CountedException o) {
		return o.count-count;
	}
}
