package cx.mccormick.canofbeats;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NumberboxDialog extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.numberbox_dialog);
		
		Intent intent = getIntent();
		final TextView title = (TextView)findViewById(R.id.title);
		title.setText(intent.getStringExtra("title"));
		this.setTitle(intent.getStringExtra("title"));

		//Context context = getApplicationContext();
		final EditText number = (EditText)findViewById(R.id.number);
		number.setText("" + intent.getFloatExtra("number", 0));
		
		Button ok = (Button)findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				result.putExtra("number", Float.parseFloat(number.getText().toString()));
				setResult(RESULT_OK, result);
				finish();
			}
		});
		
		Button cancel = (Button)findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				setResult(RESULT_CANCELED, result);
				finish();
			}
		});
	}
}
