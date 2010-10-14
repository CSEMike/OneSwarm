/*
 * Created on Mar 12, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.core3.util.test;

import java.nio.*;
import java.util.Arrays;
import java.util.Random;

// import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1;

/**
 */
public class SHA1SpeedTest {

	private static final int	BUFF_MAX_SIZE	= 4 * 1024 * 1024;

	private static final int[]	LOOPS			= { 1000000, 30000, 15000, 4000, 3000, 2000, 1200, 800 };
	private static final int[]	TESTS			= { 1, 16, 64, 256, 512, 1024, 2048, 4096 };
	
	private static final int TEST_SPEED_FACTOR = 1; // use larger numbers for less tests

	public static void main(String[] args)
	{
		
		Random rnd = new Random();

		SHA1Old oldsha = new SHA1Old();
		SHA1 newsha = new SHA1();

		ByteBuffer dBuffer = ByteBuffer.allocateDirect(BUFF_MAX_SIZE);
		ByteBuffer hBuffer = ByteBuffer.allocate(BUFF_MAX_SIZE);

		for (int i = 0; i < BUFF_MAX_SIZE; i++)
		{
			byte b = (byte) (rnd.nextInt()&0xFF);
			dBuffer.put(b);
		}
		
		dBuffer.rewind();
		hBuffer.put(dBuffer);
		hBuffer.rewind();
		dBuffer.rewind();
		

		// allow time for setting thread to high-priority
		try
		{
			System.out.println("Setting high thread priority to decrease test jitter");
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			Thread.sleep(2000);
		} catch (Exception ignore)
		{
		}

		for (int t = 0; t < TESTS.length; t++)
		{

			int buffsize = TESTS[t] * 1024;

			dBuffer.position(0);
			dBuffer.limit(buffsize);
			hBuffer.position(0);
			hBuffer.limit(buffsize);

			
			int loops = LOOPS[t]/TEST_SPEED_FACTOR;

			String info = " [" + buffsize / 1024 + "KB, " + loops + "x] = ";

			double totalMBytes = ((double) buffsize / (1024 * 1024)) * loops;

			
			long time;
			double speed;

			
			System.out.println("direct:");
			
			System.out.print("Old SHA1");
			time = System.currentTimeMillis();
			for (int i = 0; i < loops; i++)
			{
				oldsha.reset();
				oldsha.digest(dBuffer);
			}
			time = System.currentTimeMillis() - time;
			speed = totalMBytes / (time / (double)1024);
			System.out.println(info + time + " ms @ " + speed + " MiB/s");

			
			System.out.print("New SHA1 ");
			time = System.currentTimeMillis();
			for (int i = 0; i < loops; i++)
			{
				newsha.reset();
				newsha.digest(dBuffer);
			}
			time = System.currentTimeMillis() - time;
			speed = totalMBytes / (time / (double)1024);
			System.out.println(info + time + " ms @ " + speed + " MiB/s");

			System.out.println("heap:");
			
			System.out.print("Old SHA1");
			time = System.currentTimeMillis();
			for (int i = 0; i < loops; i++)
			{
				oldsha.reset();
				oldsha.digest(hBuffer);
			}
			time = System.currentTimeMillis() - time;
			speed = totalMBytes / (time / (double)1024);
			System.out.println(info + time + " ms @ " + speed + " MiB/s");

			
			System.out.print("New SHA1 ");
			time = System.currentTimeMillis();
			for (int i = 0; i < loops; i++)
			{
				newsha.reset();
				newsha.digest(hBuffer);
			}
			time = System.currentTimeMillis() - time;
			speed = totalMBytes / (time / (double)1024);
			System.out.println(info + time + " ms @ " + speed + " MiB/s");

			System.out.println();
		}
		
		
		System.out.println("performing randomized buffer windowing checks, this may take a while");
		
		byte[] oldd;
		byte[] newd;
		byte[] oldh;
		byte[] newh;
		
		int size;
		int offset;
		
		ByteBuffer dview;
		ByteBuffer hview;
		
		for(int i=0;i<LOOPS[1]/TEST_SPEED_FACTOR;i++)
		{
			size = rnd.nextInt(BUFF_MAX_SIZE);
			offset = rnd.nextInt(BUFF_MAX_SIZE-size-1);
			
			hBuffer.limit(offset+size);
			hBuffer.position(offset);
			dBuffer.limit(offset+size);
			dBuffer.position(offset);
			
			oldsha.reset();
			newsha.reset();
			oldh = oldsha.digest(hBuffer);
			newh = newsha.digest(hBuffer);
			oldsha.reset();
			newsha.reset();
			oldd = oldsha.digest(dBuffer);
			newd = newsha.digest(dBuffer);
			
			
			if(!Arrays.equals(oldh, newh) || !Arrays.equals(oldd, newd) || !Arrays.equals(oldd, oldh))
			{
				System.out.println("hash mismatch at offset: "+offset+" size: "+size);
				System.out.println("\t\t"+ByteFormatter.nicePrint(oldh));
				System.out.println("\t\t"+ByteFormatter.nicePrint(newh));
				System.out.println("\t\t"+ByteFormatter.nicePrint(oldd));
				System.out.println("\t\t"+ByteFormatter.nicePrint(newd));
			}
				
			if(hBuffer.limit() != offset+size || dBuffer.limit() != offset+size || hBuffer.position() != offset || dBuffer.position() != offset)
				System.out.println("buffer does not match its original state");
			
			dview = dBuffer.slice();
			hview = hBuffer.slice();
			
			oldsha.reset();
			newsha.reset();
			oldh = oldsha.digest(hview);
			newh = newsha.digest(hview);
			oldsha.reset();
			newsha.reset();
			oldd = oldsha.digest(dview);
			newd = newsha.digest(dview);
			
			
			if(!Arrays.equals(oldh, newh) || !Arrays.equals(oldd, newd) || !Arrays.equals(oldd, oldh))
			{
				System.out.println("(view) hash mismatch at offset: "+offset+" size: "+size);
				System.out.println("\t\t"+ByteFormatter.nicePrint(oldh));
				System.out.println("\t\t"+ByteFormatter.nicePrint(newh));
				System.out.println("\t\t"+ByteFormatter.nicePrint(oldd));
				System.out.println("\t\t"+ByteFormatter.nicePrint(newd));
			}
				
			if(hview.limit() != hview.capacity() || dview.limit() != dview.capacity() || hview.position() != 0 || dview.position() != 0)
				System.out.println("view buffer does not match its original state");
		}
		System.out.println("DONE");

	}

}
