import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.sgroupmobile.glowza.common.enum.FilterType
import com.sgroupmobile.glowza.data.model.AppFilter

object FilterPainter {
    fun drawFiltersOnCanvas(
        canvas: Canvas,
        targetWidth: Int,       // Chiều rộng của nơi cần vẽ (View hoặc Bitmap)
        targetHeight: Int,      // Chiều cao của nơi cần vẽ
        imageSourceWidth: Int,  // Kích thước ảnh gốc từ ML Kit
        imageSourceHeight: Int,
        faces: List<Face>,
        filter: AppFilter,
        context: Context,
        isFrontCamera: Boolean = false // Chỉ dùng khi vẽ trên View (Preview)
    ) {
        if (imageSourceWidth == 0 || imageSourceHeight == 0) return

        val drawable = ContextCompat.getDrawable(context, filter.filterRes) ?: return

        // Tính tỉ lệ scale dựa trên nơi cần vẽ
        val scaleX = targetWidth.toFloat() / imageSourceWidth
        val scaleY = targetHeight.toFloat() / imageSourceHeight

        faces.forEach { face ->
            // 1. Lấy tọa độ gốc
            val anchor = when (filter.type) {
                FilterType.EYES -> {
                    val left = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    val right = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                    if (left != null && right != null) PointF(
                        (left.x + right.x) / 2,
                        (left.y + right.y) / 2
                    ) else null
                }
                FilterType.TOP_HEAD -> PointF(face.boundingBox.centerX().toFloat(), face.boundingBox.top.toFloat())
                FilterType.NOSE -> face.getLandmark(FaceLandmark.NOSE_BASE)?.position
            } ?: return@forEach

            var cx = anchor.x
            var cy = anchor.y

            // 2. Chỉ lật tọa độ X nếu vẽ trên màn hình Preview (Camera trước)
            // Khi chụp ảnh lưu file, CameraX đã tự xử lý hướng nên thường không cần lật lại ở đây
            if (isFrontCamera) {
                cx = imageSourceWidth - cx
            }

            // 3. Chuyển sang tọa độ thực tế trên Canvas
            val viewCx = cx * scaleX
            val viewCy = cy * scaleY

            // 4. Tính kích thước Filter
            val faceWidth = face.boundingBox.width() * scaleX
            val filterWidth = faceWidth * filter.scaleFactor
            val filterHeight = drawable.intrinsicHeight * (filterWidth / drawable.intrinsicWidth)
            val finalCy = viewCy + (filterHeight * filter.offsetY)

            // 5. Vẽ
            canvas.save()
            canvas.rotate(face.headEulerAngleZ, viewCx, finalCy)

            val left = (viewCx - filterWidth / 2).toInt()
            val top = (finalCy - filterHeight / 2).toInt()
            drawable.setBounds(left, top, (left + filterWidth).toInt(), (top + filterHeight).toInt())
            drawable.draw(canvas)

            canvas.restore()
        }
    }
}