# ScrawlImageView
涂鸦控件，继承自ImageView
> 同这个控件：[ScrawlView](https://github.com/xuanu/ScrawlView)   

1. **修改增加两个方法**。
- 记录画过的笔迹。  `List<Line> getLines();`  
- 把笔迹绘制到图中  `drawLines(List<Line>);`  
- 设置背景图一定要使用  **setViewBackground(Bitmap);**,不然得不到图片的大小不好处理画线位置。  
