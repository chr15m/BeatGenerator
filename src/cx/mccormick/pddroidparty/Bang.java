package cx.mccormick.pddroidparty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.util.Log;

public class Bang extends Widget {
	private static final String TAG = "Bang";
	
	RectF dRect;
	boolean bang = false;
	
	public Bang(PdDroidPatchView app, String[] atomline) {
		super(app);
		
		x = Float.parseFloat(atomline[2]) / parent.patchwidth * screenwidth;
		y = Float.parseFloat(atomline[3]) / parent.patchheight * screenheight;
		w = Float.parseFloat(atomline[5]) / parent.patchwidth * screenwidth;
		h = Float.parseFloat(atomline[5]) / parent.patchheight * screenheight;
		
		sendname = atomline[9];
		receivename = atomline[10];
		label = setLabel(atomline[11]);
		labelpos[0] = Float.parseFloat(atomline[12]) / parent.patchwidth * screenwidth;
		labelpos[1] = Float.parseFloat(atomline[13]) / parent.patchheight * screenheight;

		// listen out for floats from Pd
		setupreceive();
		
		// graphics setup
		dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
	}
	
	public void draw(Canvas canvas) {
		canvas.drawLine(dRect.left + 1, dRect.top, dRect.right, dRect.top, paint);
		canvas.drawLine(dRect.left + 1, dRect.bottom, dRect.right, dRect.bottom, paint);
		canvas.drawLine(dRect.left, dRect.top + 1, dRect.left, dRect.bottom, paint);
		canvas.drawLine(dRect.right, dRect.top + 1, dRect.right, dRect.bottom, paint);
		if (bang) {
			bang = false;
			paint.setStyle(Paint.Style.FILL);
			parent.threadSafeInvalidate();
		} else {
			paint.setStyle(Paint.Style.STROKE);
		}
		canvas.drawCircle(dRect.centerX(), dRect.centerY(), w / 2, paint);
		drawLabel(canvas);
	}
	
	private void bang() {
		bang = true;
		send("bang");
	}
	
	public void touch(MotionEvent event) {
		float ex = event.getX();
		float ey = event.getY();
		if (event.getAction() == event.ACTION_DOWN && inside(ex, ey)) {
			bang();
		}
	}
	
	public void receiveAny() {
		bang();
	}
}