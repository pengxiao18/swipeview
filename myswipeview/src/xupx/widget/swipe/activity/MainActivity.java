package xupx.widget.swipe.activity;

import xupx.widget.swipe.R;
import xupx.widget.swipe.widget.SwipeView;
import xupx.widget.swipe.widget.SwipeView.SwipeLisener;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements SwipeLisener{
	private SwipeView swipeView = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		swipeView = (SwipeView) findViewById(R.id.swipeView);
		swipeView.setContentView(R.layout.content);
		swipeView.setLeftBackView(R.layout.left);
		swipeView.setRightBackView(R.layout.right);
		swipeView.setMode(SwipeView.SWIPE_MODE_BOTH);
		swipeView.setSwipeLisener(this);
	}
	
	@Override
	public void swiping(int swipe_type, float percent) {
		Log.i("data", "swiping:"+swipe_type+",percent:"+percent);
	}
	
	@Override
	public void swiped(int swipe_type) {
		Log.i("data", "swiped:"+swipe_type);
		String action = swipe_type == SwipeView.SWIPE_MODE_LEFT ? "左操作" : "右操作";
		Toast.makeText(this, "swiped:"+action, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void swipeCancel(int swipe_type) {
		Log.i("data", "swipeCancel:"+swipe_type);
		Toast.makeText(this, "swipe cancel", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void doNothing() {
	}
}
