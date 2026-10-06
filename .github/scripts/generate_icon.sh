#!/bin/bash
set -e

# Generate app icons for Android using ImageMagick
# Creates adaptive icons + legacy icons

echo "🎨 Generating app icons..."

# Ensure ImageMagick is available
if ! command -v convert &> /dev/null; then
    echo "Installing ImageMagick..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq imagemagick
fi

# Create source icon (red play button on dark background)
mkdir -p icon_tmp
cat > icon_tmp/generate_source.py << 'PYTHON'
import subprocess, os

# Create a high-res base icon using ImageMagick
size = 1024
subprocess.run([
    'convert', '-size', f'{size}x{size}',
    'xc:#0F0F0F',
    # Red gradient circle background
    '-fill', '#FF0000',
    '-draw', f'circle {size//2},{size//2} {size//2},{size//6}',
    # Inner dark circle
    '-fill', '#1F1F1F',
    '-draw', f'circle {size//2},{size//2} {size//2},{size//4}',
    # Play triangle
    '-fill', 'white',
    '-draw', f'polygon {size//3},{size//3} {size//3},{2*size//3} {2*size//3},{size//2}',
    'icon_tmp/source.png'
], check=True)
PYTHON

python3 icon_tmp/generate_source.py

# Define densities and sizes
declare -A DENSITIES=(
    ["mipmap-mdpi"]=48
    ["mipmap-hdpi"]=72
    ["mipmap-xhdpi"]=96
    ["mipmap-xxhdpi"]=144
    ["mipmap-xxxhdpi"]=192
)

# Generate legacy icons
for density in "${!DENSITIES[@]}"; do
    size=${DENSITIES[$density]}
    dir="app/src/main/res/$density"
    mkdir -p "$dir"
    convert icon_tmp/source.png -resize ${size}x${size} "$dir/ic_launcher.png"
    convert icon_tmp/source.png -resize ${size}x${size} \
        -define png:include-chunk=none "$dir/ic_launcher_round.png"
    echo "✓ Generated $density (${size}x${size})"
done

# Generate adaptive icon XML
mkdir -p app/src/main/res/mipmap-anydpi-v26

cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
XML

cp app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml \
   app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml

# Generate adaptive icon layers
mkdir -p app/src/main/res/drawable

# Background (dark with red accent)
cat > app/src/main/res/drawable/ic_launcher_background.xml << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0F0F0F"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#FF0000"
        android:pathData="M54,20 A34,34 0 1,1 54,88 A34,34 0 1,1 54,20" />
</vector>
XML

# Foreground (play button)
cat > app/src/main/res/drawable/ic_launcher_foreground.xml << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#1F1F1F"
        android:pathData="M54,30 A24,24 0 1,1 54,78 A24,24 0 1,1 54,30" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,38 L44,70 L70,54 Z" />
</vector>
XML

# Cleanup
rm -rf icon_tmp

echo "✅ All icons generated successfully!"
