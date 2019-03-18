package cx.mccormick.canofbeats;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class Canvasrect extends Widget {
	private static final String TAG = "Canvas";
	
	WImage image = new WImage();
	
	public Canvasrect(PdDroidPatchView app, String[] atomline) {
		super(app);
		
		float x = Float.parseFloat(atomline[2]) ;
		float y = Float.parseFloat(atomline[3]) ;
		float w = Float.parseFloat(atomline[6]) ;
		float h = Float.parseFloat(atomline[7]) ;
		sendname = atomline[8];
		receivename = atomline[9];
		label = setLabel(atomline[10]);
		labelpos[0] = Float.parseFloat(atomline[11]);
		labelpos[1] = Float.parseFloat(atomline[12]);
		labelfont = Integer.parseInt(atomline[13]);
		labelsize = (int)(Float.parseFloat(atomline[14]));
		bgcolor = getColor(Integer.parseInt(atomline[15]));
		labelcolor = getColor(Integer.parseInt(atomline[16]));
		
		setupreceive();
		dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
		image.load(TAG, null, receivename);
		
		if(receivename.equals("ViewPort")) {
			parent.viewX=(int)dRect.left;
			parent.viewY=(int)dRect.top;
			parent.viewW=(int)dRect.width();
			parent.viewH=(int)dRect.height();
		}
	}
	
		
	public void draw(Canvas canvas) {
		if(receivename.equals("ViewPort")) return;
		if (image.draw(canvas)) {
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(bgcolor);			
			canvas.drawRect(dRect.left, dRect.top, dRect.right, dRect.bottom, paint); 
			drawLabel(canvas);
		}
	}
	
	public void receiveMessage(String symbol, Object... args) {
		
		if (symbol.equals("vis_size") && args.length > 1 && args[0].getClass().equals(Float.class) && args[1].getClass().equals(Float.class)) {
			float w = Float.parseFloat(args[0].toString());
			float h = Float.parseFloat(args[1].toString());
			
			dRect.right = dRect.left + w;
			dRect.bottom = dRect.top + h;
			
		} 
		else widgetreceiveSymbol(symbol,args);
		
		if(receivename.equals("ViewPort")) {
			if (symbol.equals("vis_size") || symbol.equals("pos") ) {
				parent.viewX=(int)dRect.left;
				parent.viewY=(int)dRect.top;
				parent.viewW=(int)dRect.width();
				parent.viewH=(int)dRect.height();
			}
		}
	}
	
}
