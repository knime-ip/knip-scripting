package org.knime.knip.scripting.playground;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

public class MyClass<T extends RealType<T>> implements Command {
 	@Parameter(type = ItemIO.INPUT)
 	private ImgPlus<T> in_img;

 	@Parameter(type = ItemIO.OUTPUT)
 	private ImgPlus<T> out_img;

	public void run() {
		Img<T> in = in_img.getImg();
		T t = in.cursor().next();

		long[] dimensions = new long[in.numDimensions()];
		in.dimensions(dimensions);

		for (int i = 0; i < dimensions.length; ++i) {
			dimensions[i] >>= 1;
		}
		
		Img<T> img = in.factory().create(dimensions, t);
		
		Cursor<T> cin = in.cursor();
		Cursor<T> cout = img.cursor();
		
		// fill img with every second pixel in dimensions 0 and 1
		while(cout.hasNext()) {
			int y = 0; // skip all uneven rows
			if (((y = cin.getIntPosition(1)) & 1)!=0) {
				// skip row
				while(cin.getIntPosition(1) == y) {
					cin.fwd();
				}
			}
			
			// set pixel of cout to pixel in 
			cout.next().set(cin.next());
			// skip one
			cin.fwd(); 
		}
		
		out_img = new ImgPlus<T>(img);
		return;
	}
}
