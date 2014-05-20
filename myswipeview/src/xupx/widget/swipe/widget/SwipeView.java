package xupx.widget.swipe.widget;

import xupx.widget.swipe.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

/**
 * 自定义 左右滑动item
 * 
 * @author xupx
 * 
 */
public class SwipeView extends FrameLayout implements OnGlobalLayoutListener{
	
	// swipe 监听器
	public interface SwipeLisener{
		public void swiping(int swipe_type,float percent);
		public void swiped(int swipe_type);
		public void swipeCancel(int swipe_type);
		public void doNothing();
	}
	
	// swipe mode
	public final static int SWIPE_MODE_NONE = 0;
	public final static int SWIPE_MODE_BOTH = 1;
	public final static int SWIPE_MODE_RIGHT = 2;
	public final static int SWIPE_MODE_LEFT = 3;

	// view,left、content、right
	private View leftBackView;
	private View rightBackView;
	private View contentView;
	
	// 偏移量
	private float leftOffset = 0;
	private float rightOffset = 0;

	// swipe mode
	private int swipeMode = SWIPE_MODE_NONE;
	
	// 是否正在弹回
	private boolean isBacking = false;
	
	// swipe 监听器
	private SwipeLisener swipeLisener = null;
	
	//当前swipe 类型
	private int swipe_type = SWIPE_MODE_NONE;

	public SwipeView(Context context) {
		this(context, null);
	}

	public SwipeView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SwipeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray styled = getContext().obtainStyledAttributes(attrs,R.styleable.swipeView);

			swipeMode = styled.getInt(R.styleable.swipeView_swipeMode,SWIPE_MODE_NONE);
			leftOffset = styled.getDimension(R.styleable.swipeView_leftOffset,0);
			rightOffset = styled.getDimension(R.styleable.swipeView_rightOffset, 0);

			int contentView = styled.getResourceId(R.styleable.swipeView_contentView, -1);
			int leftBackView = styled.getResourceId(R.styleable.swipeView_leftBackView, -1);
			int rightBackView = styled.getResourceId(R.styleable.swipeView_rightBackView, -1);

			if (contentView != -1) {
				setContentView(contentView);
			}
			if (leftBackView != -1) {
				setLeftBackView(leftBackView);
			}
			if (rightBackView != -1) {
				setRightBackView(rightBackView);
			}

			styled.recycle();
		}
		// 添加布局完成监听器
		this.getViewTreeObserver().addOnGlobalLayoutListener(this);
	}
	
	// 手势识别
	private static final int GESTURE_NO = 0;
	private static final int GESTURE_UP = 1;
	private static final int GESTURE_LEFT = 2;
	private static final int GESTURE_RIGHT = 3;
	
	private boolean isDone = false;
	private int current_gesture = 0;
	private int min_area = 30;
	private int max_area_left = 100;
	private int max_area_right = 100;
	
	private PointF startPoint = new PointF();
	private PointF movePoint = new PointF();
	// content view 初始位置
	private int start_l,start_t,start_r,start_b;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isBacking){
			return true;
		}
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch(action){
		case MotionEvent.ACTION_DOWN:
			startPoint.set(event.getX(), event.getY());
			getContentViewPoi();
			break;
		case MotionEvent.ACTION_MOVE:
			if(getVisibility() != View.VISIBLE || isBacking){
				break;
			}
			if(startPoint.x==0){
				startPoint.set(event.getX(), event.getY());
				getContentViewPoi();
				break;
			}
			movePoint.set(event.getX(),event.getY());
			// 偏移量算法不准确，应该算实际偏移量
			// y方向偏移
			float yd = movePoint.y - startPoint.y;
			// x方向偏移
			float xd = movePoint.x - startPoint.x;
			//float xd = contentView.getLeft() - start_l;
			// 识别手势
			recognitionGesture(yd, xd);
			// 判断手势是否完成
			finishGesture(yd, xd,event);
			// 移动 content view
			moveContentView(event);
			break;
		case MotionEvent.ACTION_UP:
			startPoint.set(0, 0);
			// 反弹
			moveToBack();
			break;
		}
		return true;
	}
	
	/**
	 * 识别手势
	 * @param yd
	 * @param xd
	 */
	private void recognitionGesture(float yd,float xd){
		float ads_yd = Math.abs(yd);
		float ads_xd = Math.abs(xd);
		// 垂直手势
		if(ads_yd > min_area && ads_xd < min_area){
			current_gesture = yd < 0 ? GESTURE_UP : GESTURE_NO;
			return;
		}
		// 水平手势
		if(ads_xd > min_area && ads_yd < min_area){
			current_gesture = xd > 0 ? GESTURE_RIGHT : GESTURE_LEFT;
			return;
		}
	}
	
	/**
	 * 判断手势是否完成
	 * @param yd
	 * @param xd
	 */
	private void finishGesture(float yd,float xd,MotionEvent event){
		if(current_gesture == GESTURE_NO){
			return;
		}
		//float ads_yd = Math.abs(yd);
		float ads_xd = Math.abs(xd);
		switch (current_gesture) {
		case GESTURE_LEFT:
			isDone = ads_xd > max_area_left;
			// 右操作
			swipe_type = SWIPE_MODE_RIGHT;
			break;
		case GESTURE_RIGHT:
			isDone = ads_xd >= max_area_right;
			// 左操作
			swipe_type = SWIPE_MODE_LEFT;
			break;
		}
		
	}
	
	/**
	 * 获取 content view 位置
	 */
	private void getContentViewPoi(){
		start_l = contentView.getLeft();
		start_t = contentView.getTop();
		start_r = contentView.getRight();
		start_b = contentView.getBottom();
	}
	
	/**
	 * 移动 content view
	 * @param event
	 */
	private void moveContentView(MotionEvent event){
		// 手指滑动偏移量
		int dx = (int)(movePoint.x - startPoint.x);
		int abs_dx = Math.abs(dx);
		
		// content view 实际偏移量
		int left = contentView.getLeft(); 
		int abs_left = Math.abs(left);
		
		switch(getMode()){
		case SWIPE_MODE_NONE:// 如果默认mode ,则不滑动
			return;
		case SWIPE_MODE_BOTH:// 如果both
			if((left < 0 && abs_left > rightOffset) 
					|| (left > 0 && abs_left > leftOffset)){
				return;
			}
			break;
		case SWIPE_MODE_RIGHT://向左边滑动,右边swipe显示
			if(left >= 0 || abs_left > rightOffset){
				return;
			}
			break;
		case SWIPE_MODE_LEFT://向右边滑动，左边swipe显示
			if(left <= 0 || abs_left > leftOffset){
				return;
			}
			break;
		}
		// 进度百分数
		float precent = 0.0f;
		// 调整偏差
		if(dx > 0){
			dx = dx > leftOffset ? (int)leftOffset : dx;
			// 计算进度
			precent = Math.abs(dx) / leftOffset;
		}else{
			dx = abs_dx > rightOffset ? -(int)rightOffset : dx;
			// 计算进度
			precent = Math.abs(dx) / rightOffset;
		}
		contentView.layout(start_l+dx, start_t, start_r+dx, start_b);
		// 进度回调函数
		if(swipeLisener != null){
			swipeLisener.swiping(swipe_type,precent);
		}
	}
	
	/**
	 * 弹回动画
	 */
	private void moveToBack(){
		// 如果手势还没有识别
		if(current_gesture==GESTURE_NO){
			if(swipeLisener != null){
				// 回复原位
				contentView.layout(start_l, start_t, start_r, start_b);
				// 手势状态还原
				isDone = false;
				current_gesture = GESTURE_NO;
				swipe_type = SWIPE_MODE_NONE;
				// do nothing
				swipeLisener.doNothing();
				return;
			}
		}
		// 水平移动移动距离
		int dx = start_l - contentView.getLeft();
		TranslateAnimation ani = new TranslateAnimation(0, dx, 0, 0);
		ani.setDuration(300);
		ani.setInterpolator(new DecelerateInterpolator());
		ani.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				contentView.clearAnimation();
				contentView.layout(start_l, start_t, start_r, start_b);
				isBacking = false;
				// 回调函数
				if(swipeLisener != null){
					if(isDone){
						swipeLisener.swiped(swipe_type);
					}else{
						swipeLisener.swipeCancel(swipe_type);
					}
				}
				// 手势状态还原
				isDone = false;
				current_gesture = GESTURE_NO;
				swipe_type = SWIPE_MODE_NONE;
			}
		});
		contentView.startAnimation(ani);
		isBacking = true;
	}
	
	public SwipeLisener getSwipeLisener() {
		return swipeLisener;
	}

	public void setSwipeLisener(SwipeLisener swipeLisener) {
		this.swipeLisener = swipeLisener;
	}

	public void setContentView(int resId) {
		setContentView(LayoutInflater.from(getContext()).inflate(resId, null));
	}

	public void setLeftBackView(int resId) {
		setLeftBackView(LayoutInflater.from(getContext()).inflate(resId, null));
	}

	public void setRightBackView(int resId) {
		setRightBackView(LayoutInflater.from(getContext()).inflate(resId, null));
	}

	public void setContentView(View contentView) {
		if (this.contentView != null) {
			removeView(contentView);
		}
		this.contentView = contentView;
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		addView(this.contentView,params);
	}

	public void setLeftBackView(View leftBackView) {
		if (this.leftBackView != null) {
			removeView(leftBackView);
		}
		//leftBackView.setVisibility(View.GONE);
		this.leftBackView = leftBackView;
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
		addView(this.leftBackView,params);
		if(contentView != null){
			contentView.bringToFront();
		}
	}

	public void setRightBackView(View rightBackView) {
		if (this.rightBackView != null) {
			removeView(rightBackView);
		}
		this.rightBackView = rightBackView;
		//rightBackView.setVisibility(View.GONE);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
		addView(this.rightBackView,params);
		if(contentView != null){
			contentView.bringToFront();
		}
	}

	public View getContentView() {
		return this.contentView;
	}

	public View getLeftBackView() {
		return this.leftBackView;
	}

	public View getRightBackView() {
		return this.rightBackView;
	}

	public void setMode(int mode){
		this.swipeMode = mode;
	}
	
	public int getMode(){
		return this.swipeMode;
	}

	public float getLeftOffset() {
		return leftOffset;
	}

	public void setLeftOffset(float leftOffset) {
		this.leftOffset = leftOffset;
		max_area_left = (int)leftOffset;
	}

	public float getRightOffset() {
		return rightOffset;
	}

	public void setRightOffset(float rightOffset) {
		this.rightOffset = rightOffset;
		max_area_right = (int) rightOffset;
	}
	
	@Override
	public void onGlobalLayout() {
		if(leftBackView != null && leftOffset ==0){
			leftOffset = leftBackView.getWidth();
		}
		if(rightBackView != null && rightOffset ==0){
			rightOffset = rightBackView.getWidth();
		}
		// 设置左右 识别 区域
		max_area_left = (int)leftOffset;
		max_area_right = (int)rightOffset;
	}
}
