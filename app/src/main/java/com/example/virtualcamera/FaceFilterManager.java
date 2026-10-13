package com.example.virtualcamera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

/**
 * Face Filter Manager - Applies Snapchat-like filters to faces
 * Uses basic image processing for demo purposes
 * For production, consider using:
 * - Google ML Kit Face Detection
 * - Firebase ML Vision
 * - OpenCV
 */
public class FaceFilterManager {
    
    // Filter types
    public static final int FILTER_NONE = 0;
    public static final int FILTER_DOG_EARS = 1;
    public static final int FILTER_GLASSES = 2;
    public static final int FILTER_HAT = 3;
    public static final int FILTER_CROWN = 4;
    public static final int FILTER_BEARD = 5;
    public static final int FILTER_MUSTACHE = 6;
    public static final int FILTER_HEART_EYES = 7;
    public static final int FILTER_FIRE_CROWN = 8;
    public static final int FILTER_RAINBOW = 9;
    
    private int currentFilter = FILTER_NONE;
    private float filterIntensity = 1.0f;
    
    // Face detection (simplified for demo)
    // In production, use ML Kit or OpenCV for accurate face detection
    
    /**
     * Apply face filter to bitmap
     */
    public Bitmap applyFilter(Bitmap source) {
        if (source == null || currentFilter == FILTER_NONE) {
            return source;
        }
        
        // For demo, we'll detect faces using simple color/pattern matching
        // and apply filters at approximate face positions
        
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        
        // Detect faces (simplified)
        Face[] faces = detectFaces(source);
        
        for (Face face : faces) {
            applyFilterToFace(canvas, face);
        }
        
        return result;
    }
    
    /**
     * Simplified face detection (for demo only)
     * In production, use ML Kit: FirebaseVisionFaceDetector
     */
    private Face[] detectFaces(Bitmap bitmap) {
        // This is a placeholder - real face detection requires ML
        // For demo, we'll return a single face in the center
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Assume face is in the center 1/3 of the image
        int faceWidth = width / 3;
        int faceHeight = height / 3;
        int faceX = width / 2 - faceWidth / 2;
        int faceY = height / 2 - faceHeight / 2;
        
        Face face = new Face();
        face.rect = new RectF(faceX, faceY, faceX + faceWidth, faceY + faceHeight);
        face.landmarks = new Face.Landmark[5];
        
        // Approximate landmark positions
        face.landmarks[0] = new Face.Landmark(faceX + faceWidth / 2, faceY + faceHeight / 4); // Nose
        face.landmarks[1] = new Face.Landmark(faceX + faceWidth / 4, faceY + faceHeight / 3); // Left eye
        face.landmarks[2] = new Face.Landmark(faceX + 3 * faceWidth / 4, faceY + faceHeight / 3); // Right eye
        face.landmarks[3] = new Face.Landmark(faceX + faceWidth / 4, faceY + 2 * faceHeight / 3); // Left mouth
        face.landmarks[4] = new Face.Landmark(faceX + 3 * faceWidth / 4, faceY + 2 * faceHeight / 3); // Right mouth
        
        return new Face[]{face};
    }
    
    /**
     * Apply filter to a detected face
     */
    private void applyFilterToFace(Canvas canvas, Face face) {
        switch (currentFilter) {
            case FILTER_DOG_EARS:
                drawDogEars(canvas, face);
                break;
            case FILTER_GLASSES:
                drawGlasses(canvas, face);
                break;
            case FILTER_HAT:
                drawHat(canvas, face);
                break;
            case FILTER_CROWN:
                drawCrown(canvas, face);
                break;
            case FILTER_BEARD:
                drawBeard(canvas, face);
                break;
            case FILTER_MUSTACHE:
                drawMustache(canvas, face);
                break;
            case FILTER_HEART_EYES:
                drawHeartEyes(canvas, face);
                break;
            case FILTER_FIRE_CROWN:
                drawFireCrown(canvas, face);
                break;
            case FILTER_RAINBOW:
                drawRainbow(canvas, face);
                break;
        }
    }
    
    /**
     * Draw dog ears on face
     */
    private void drawDogEars(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.rgb(200, 150, 100));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        Face.Landmark leftEye = face.landmarks[1];
        Face.Landmark rightEye = face.landmarks[2];
        
        float earWidth = face.rect.width() / 4;
        float earHeight = face.rect.height() / 3;
        
        // Left ear
        Path leftEar = new Path();
        leftEar.moveTo(leftEye.x - earWidth / 2, leftEye.y - earHeight / 2);
        leftEar.lineTo(leftEye.x, leftEye.y - earHeight);
        leftEar.lineTo(leftEye.x + earWidth / 2, leftEye.y - earHeight / 2);
        leftEar.close();
        canvas.drawPath(leftEar, paint);
        
        // Right ear
        Path rightEar = new Path();
        rightEar.moveTo(rightEye.x - earWidth / 2, rightEye.y - earHeight / 2);
        rightEar.lineTo(rightEye.x, rightEye.y - earHeight);
        rightEar.lineTo(rightEye.x + earWidth / 2, rightEye.y - earHeight / 2);
        rightEar.close();
        canvas.drawPath(rightEar, paint);
        
        // Inner ears
        paint.setColor(Color.rgb(255, 200, 150));
        canvas.drawPath(leftEar, paint);
        canvas.drawPath(rightEar, paint);
    }
    
    /**
     * Draw glasses on face
     */
    private void drawGlasses(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);
        
        Face.Landmark leftEye = face.landmarks[1];
        Face.Landmark rightEye = face.landmarks[2];
        
        float eyeDistance = rightEye.x - leftEye.x;
        float glassWidth = eyeDistance * 1.5f;
        float glassHeight = face.rect.height() / 6;
        
        // Left lens
        RectF leftLens = new RectF(
            leftEye.x - eyeDistance / 4,
            leftEye.y - glassHeight / 2,
            leftEye.x + eyeDistance / 4,
            leftEye.y + glassHeight / 2
        );
        canvas.drawRoundRect(leftLens, 10, 10, paint);
        
        // Right lens
        RectF rightLens = new RectF(
            rightEye.x - eyeDistance / 4,
            rightEye.y - glassHeight / 2,
            rightEye.x + eyeDistance / 4,
            rightEye.y + glassHeight / 2
        );
        canvas.drawRoundRect(rightLens, 10, 10, paint);
        
        // Bridge
        canvas.drawLine(
            leftEye.x + eyeDistance / 4,
            leftEye.y,
            rightEye.x - eyeDistance / 4,
            rightEye.y,
            paint
        );
    }
    
    /**
     * Draw hat on face
     */
    private void drawHat(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.rgb(100, 50, 0));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        RectF faceRect = face.rect;
        
        // Hat brim
        RectF brim = new RectF(
            faceRect.left - faceRect.width() / 4,
            faceRect.top - faceRect.height() / 6,
            faceRect.right + faceRect.width() / 4,
            faceRect.top
        );
        canvas.drawRect(brim, paint);
        
        // Hat top
        Path top = new Path();
        top.moveTo(faceRect.left - faceRect.width() / 4, faceRect.top - faceRect.height() / 6);
        top.lineTo(faceRect.left + faceRect.width() / 4, faceRect.top - faceRect.height() / 3);
        top.lineTo(faceRect.right - faceRect.width() / 4, faceRect.top - faceRect.height() / 3);
        top.lineTo(faceRect.right + faceRect.width() / 4, faceRect.top - faceRect.height() / 6);
        top.close();
        canvas.drawPath(top, paint);
    }
    
    /**
     * Draw crown on face
     */
    private void drawCrown(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        RectF faceRect = face.rect;
        
        // Crown base
        RectF base = new RectF(
            faceRect.left - faceRect.width() / 6,
            faceRect.top - faceRect.height() / 4,
            faceRect.right + faceRect.width() / 6,
            faceRect.top - faceRect.height() / 6
        );
        canvas.drawRect(base, paint);
        
        // Crown points
        Path points = new Path();
        points.moveTo(faceRect.left - faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        points.lineTo(faceRect.left, faceRect.top - faceRect.height() / 3);
        points.lineTo(faceRect.left + faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        points.moveTo(faceRect.left + faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        points.lineTo(faceRect.left + faceRect.width() / 3, faceRect.top - faceRect.height() / 3);
        points.lineTo(faceRect.left + faceRect.width() / 2, faceRect.top - faceRect.height() / 6);
        points.moveTo(faceRect.left + faceRect.width() / 2, faceRect.top - faceRect.height() / 6);
        points.lineTo(faceRect.right - faceRect.width() / 3, faceRect.top - faceRect.height() / 3);
        points.lineTo(faceRect.right - faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        points.moveTo(faceRect.right - faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        points.lineTo(faceRect.right, faceRect.top - faceRect.height() / 3);
        points.lineTo(faceRect.right + faceRect.width() / 6, faceRect.top - faceRect.height() / 6);
        canvas.drawPath(points, paint);
    }
    
    /**
     * Draw beard on face
     */
    private void drawBeard(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.rgb(100, 50, 0));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        RectF faceRect = face.rect;
        Face.Landmark leftMouth = face.landmarks[3];
        Face.Landmark rightMouth = face.landmarks[4];
        
        // Beard shape
        Path beard = new Path();
        beard.moveTo(leftMouth.x, leftMouth.y + 10);
        beard.lineTo(leftMouth.x - 20, leftMouth.y + faceRect.height() / 3);
        beard.lineTo(rightMouth.x + 20, rightMouth.y + faceRect.height() / 3);
        beard.lineTo(rightMouth.x, rightMouth.y + 10);
        beard.lineTo(leftMouth.x, leftMouth.y + 10);
        beard.close();
        canvas.drawPath(beard, paint);
    }
    
    /**
     * Draw mustache on face
     */
    private void drawMustache(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        Face.Landmark leftMouth = face.landmarks[3];
        Face.Landmark rightMouth = face.landmarks[4];
        Face.Landmark nose = face.landmarks[0];
        
        // Mustache shape
        Path mustache = new Path();
        mustache.moveTo(leftMouth.x, leftMouth.y - 5);
        mustache.lineTo(nose.x - 10, nose.y + 10);
        mustache.lineTo(nose.x + 10, nose.y + 10);
        mustache.lineTo(rightMouth.x, rightMouth.y - 5);
        mustache.lineTo(leftMouth.x, leftMouth.y - 5);
        mustache.close();
        canvas.drawPath(mustache, paint);
    }
    
    /**
     * Draw heart eyes on face
     */
    private void drawHeartEyes(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        Face.Landmark leftEye = face.landmarks[1];
        Face.Landmark rightEye = face.landmarks[2];
        
        // Left heart eye
        drawHeart(canvas, leftEye.x, leftEye.y, 15, paint);
        
        // Right heart eye
        drawHeart(canvas, rightEye.x, rightEye.y, 15, paint);
    }
    
    /**
     * Draw a heart shape
     */
    private void drawHeart(Canvas canvas, float cx, float cy, float size, Paint paint) {
        Path heart = new Path();
        heart.moveTo(cx, cy - size);
        heart.cubicTo(cx - size, cy - size, cx - size, cy, cx, cy + size / 2);
        heart.cubicTo(cx, cy, cx + size, cy, cx + size, cy - size);
        heart.cubicTo(cx + size, cy - size, cx, cy - size * 1.5f, cx, cy - size);
        heart.close();
        canvas.drawPath(heart, paint);
    }
    
    /**
     * Draw fire crown on face
     */
    private void drawFireCrown(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        RectF faceRect = face.rect;
        
        // Fire flames
        paint.setColor(Color.RED);
        Path flame1 = new Path();
        flame1.moveTo(faceRect.left - faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame1.lineTo(faceRect.left, faceRect.top - faceRect.height() / 2);
        flame1.lineTo(faceRect.left + faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame1.close();
        canvas.drawPath(flame1, paint);
        
        paint.setColor(Color.ORANGE);
        Path flame2 = new Path();
        flame2.moveTo(faceRect.left + faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame2.lineTo(faceRect.left + faceRect.width() / 3, faceRect.top - faceRect.height() / 2);
        flame2.lineTo(faceRect.left + faceRect.width() / 2, faceRect.top - faceRect.height() / 4);
        flame2.close();
        canvas.drawPath(flame2, paint);
        
        paint.setColor(Color.YELLOW);
        Path flame3 = new Path();
        flame3.moveTo(faceRect.left + faceRect.width() / 2, faceRect.top - faceRect.height() / 4);
        flame3.lineTo(faceRect.right - faceRect.width() / 3, faceRect.top - faceRect.height() / 2);
        flame3.lineTo(faceRect.right - faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame3.close();
        canvas.drawPath(flame3, paint);
        
        paint.setColor(Color.RED);
        Path flame4 = new Path();
        flame4.moveTo(faceRect.right - faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame4.lineTo(faceRect.right, faceRect.top - faceRect.height() / 2);
        flame4.lineTo(faceRect.right + faceRect.width() / 6, faceRect.top - faceRect.height() / 4);
        flame4.close();
        canvas.drawPath(flame4, paint);
    }
    
    /**
     * Draw rainbow effect
     */
    private void drawRainbow(Canvas canvas, Face face) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        
        RectF faceRect = face.rect;
        
        // Rainbow arcs
        int[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.rgb(75, 0, 130), Color.rgb(148, 0, 211)};
        
        for (int i = 0; i < colors.length; i++) {
            paint.setColor(colors[i]);
            float radius = faceRect.width() * (i + 1) / 2f;
            canvas.drawArc(
                new RectF(
                    faceRect.centerX() - radius,
                    faceRect.top - radius,
                    faceRect.centerX() + radius,
                    faceRect.top + radius
                ),
                180, 180, false, paint);
        }
    }
    
    /**
     * Set current filter
     */
    public void setFilter(int filter) {
        this.currentFilter = filter;
    }
    
    /**
     * Get current filter
     */
    public int getFilter() {
        return currentFilter;
    }
    
    /**
     * Set filter intensity
     */
    public void setIntensity(float intensity) {
        this.filterIntensity = Math.max(0, Math.min(1, intensity));
    }
    
    /**
     * Get filter intensity
     */
    public float getIntensity() {
        return filterIntensity;
    }
    
    /**
     * Get filter name
     */
    public static String getFilterName(int filter) {
        switch (filter) {
            case FILTER_NONE:
                return "None";
            case FILTER_DOG_EARS:
                return "Dog Ears";
            case FILTER_GLASSES:
                return "Glasses";
            case FILTER_HAT:
                return "Hat";
            case FILTER_CROWN:
                return "Crown";
            case FILTER_BEARD:
                return "Beard";
            case FILTER_MUSTACHE:
                return "Mustache";
            case FILTER_HEART_EYES:
                return "Heart Eyes";
            case FILTER_FIRE_CROWN:
                return "Fire Crown";
            case FILTER_RAINBOW:
                return "Rainbow";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get all filter names
     */
    public static String[] getAllFilterNames() {
        return new String[]{
            "None",
            "Dog Ears",
            "Glasses",
            "Hat",
            "Crown",
            "Beard",
            "Mustache",
            "Heart Eyes",
            "Fire Crown",
            "Rainbow"
        };
    }
    
    /**
     * Get all filter types
     */
    public static int[] getAllFilterTypes() {
        return new int[]{
            FILTER_NONE,
            FILTER_DOG_EARS,
            FILTER_GLASSES,
            FILTER_HAT,
            FILTER_CROWN,
            FILTER_BEARD,
            FILTER_MUSTACHE,
            FILTER_HEART_EYES,
            FILTER_FIRE_CROWN,
            FILTER_RAINBOW
        };
    }
    
    /**
     * Reset filter
     */
    public void reset() {
        currentFilter = FILTER_NONE;
        filterIntensity = 1.0f;
    }
    
    /**
     * Face class for detection results
     */
    public static class Face {
        public RectF rect;
        public Landmark[] landmarks;
        
        public static class Landmark {
            public float x;
            public float y;
            
            public Landmark(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}
