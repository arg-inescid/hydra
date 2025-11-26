#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

DATA_IP="172.18.0.1"
DATA_PORT=8000
DATA_ADDRESS="http://$DATA_IP:$DATA_PORT"

BENCHMARK_HOME_DIR="$ARGO_HOME/../faastion/benchmarks/src/java/SeBS"

FAASTION_BENCHMARKS=(fa_cl  fa_dn  fa_dh  fa_bf  fa_co  fa_ms  fa_pr  fa_th  fa_up  fa_vp) # full list
FAASTION_BENCHMARKS=(fa_dn  fa_dh  fa_bf  fa_co  fa_ms  fa_pr  fa_th  fa_up) # working


declare -A BENCHMARK_CODE
BENCHMARK_CODE[fa_cl]="$DATA_ADDRESS/apps/libclassify.zip"
BENCHMARK_CODE[fa_dn]="$DATA_ADDRESS/apps/libdna.zip"
BENCHMARK_CODE[fa_dh]="$DATA_ADDRESS/apps/libdynamic-html.zip"
BENCHMARK_CODE[fa_bf]="$DATA_ADDRESS/apps/libbfs.zip"
BENCHMARK_CODE[fa_co]="$DATA_ADDRESS/apps/libzip.zip"
BENCHMARK_CODE[fa_ms]="$DATA_ADDRESS/apps/libmst.zip"
BENCHMARK_CODE[fa_pr]="$DATA_ADDRESS/apps/libpagerank.zip"
BENCHMARK_CODE[fa_th]="$DATA_ADDRESS/apps/libthumbnail.zip"
BENCHMARK_CODE[fa_up]="$DATA_ADDRESS/apps/libuploader.zip"
BENCHMARK_CODE[fa_vp]="$DATA_ADDRESS/apps/libvideoprocessing.zip"


declare -A BENCHMARK_CODE_PLUGIN
BENCHMARK_CODE_PLUGIN[fa_cl]="$DATA_ADDRESS/apps/libclassify-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_dn]="$DATA_ADDRESS/apps/libdna-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_dh]="$DATA_ADDRESS/apps/libdynamic-html-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_bf]="$DATA_ADDRESS/apps/libbfs-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_co]="$DATA_ADDRESS/apps/libzip-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_ms]="$DATA_ADDRESS/apps/libmst-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_pr]="$DATA_ADDRESS/apps/libpagerank-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_th]="$DATA_ADDRESS/apps/libthumbnail-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_up]="$DATA_ADDRESS/apps/libuploader-plugin.zip"
BENCHMARK_CODE_PLUGIN[fa_vp]="$DATA_ADDRESS/apps/libvideoprocessing-plugin.zip"


declare -A BENCHMARK_ENTRYPOINTS
BENCHMARK_ENTRYPOINTS[fa_cl]="com.classify.Classify"
BENCHMARK_ENTRYPOINTS[fa_dn]="com.dna.DNAVisualization"
BENCHMARK_ENTRYPOINTS[fa_dh]="com.dynamic_html.DynamicHTML"
BENCHMARK_ENTRYPOINTS[fa_bf]="com.jni.BFS"
BENCHMARK_ENTRYPOINTS[fa_co]="com.jni.ZIPCompression"
BENCHMARK_ENTRYPOINTS[fa_ms]="com.jni.MST"
BENCHMARK_ENTRYPOINTS[fa_pr]="com.jni.PageRank"
BENCHMARK_ENTRYPOINTS[fa_th]="com.thumbnail.Thumbnail"
BENCHMARK_ENTRYPOINTS[fa_up]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[fa_vp]="com.videoprocessing.VideoProcessing"


declare -A BENCHMARK_PAYLOADS
BENCHMARK_PAYLOADS[fa_cl]="{}"
BENCHMARK_PAYLOADS[fa_dn]="{}"
BENCHMARK_PAYLOADS[fa_dh]="{}"
BENCHMARK_PAYLOADS[fa_bf]="{}"
BENCHMARK_PAYLOADS[fa_co]="{}"
BENCHMARK_PAYLOADS[fa_ms]="{}"
BENCHMARK_PAYLOADS[fa_pr]="{}"
BENCHMARK_PAYLOADS[fa_th]="{}"
BENCHMARK_PAYLOADS[fa_up]="{}"
BENCHMARK_PAYLOADS[fa_vp]="{}"
