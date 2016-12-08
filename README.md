# ScrawlImageView
涂鸦控件，继承自ImageView
> 同这个控件：[ScrawlView](https://github.com/xuanu/ScrawlView)   
  
1. 在项目的要目录build.gradle中添加：    
  
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```    

2. 添加依赖:  
   
```
dependencies {
	        compile 'com.github.xuanu:ScrawlImageView:0.0.9'
	}
```  
   
3.   
   
***  

1. **修改增加两个方法**。  
- 设置背景图一定要使用  **setViewBackground(Bitmap);**,不然得不到图片的大小不好处理画线位置。    
- 记录画过的笔迹。  `List<Line> getLines();`    
- 把笔迹绘制到图中  `drawLines(List<Line>);`    
- 增加方法：把文字画到图中 `drawText(int index,float x,float y,String text)`  
- 设置文字颜色和大小： `setTextSize(int),setTextColor(int)`，没有判断是否为色值    
- 取当前控件截图： `Bitmap getScreenShot()`    
- 设置drawText()显示文字个数： `void setDrawTextCount(int count)`  
效果：  
![image](https://github.com/xuanu/ScrawlImageView/raw/master/screenshots/device-2016-12-07-161347.png)  

