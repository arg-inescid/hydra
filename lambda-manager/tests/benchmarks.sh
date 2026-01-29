#!/bin/bash

DATA_IP="172.18.0.1"
DATA_PORT=8000
DATA_ADDRESS="http://$DATA_IP:$DATA_PORT"

HY_JV_BENCHMARKS=(hy_jv_hw  hy_jv_fh  hy_jv_cl  hy_jv_hr  hy_jv_vp)
HY_JV_BENCHMARKS=(hy_jv_hw)
HY_PY_BENCHMARKS=(hy_py_hw  hy_py_ms  hy_py_bf  hy_py_pr  hy_py_dn  hy_py_dh  hy_py_co  hy_py_th  hy_py_vp  hy_py_up)
HY_JS_BENCHMARKS=(hy_js_hw  hy_js_dh  hy_js_th  hy_js_up)
HY_JS_BENCHMARKS=(hy_js_hw  hy_js_dh  hy_js_up)
HY_BENCHMARKS=("${HY_JV_BENCHMARKS[@]}" "${HY_PY_BENCHMARKS[@]}" "${HY_JS_BENCHMARKS[@]}")
HY_BENCHMARKS=("${HY_JV_BENCHMARKS[@]}")

#OW_JV_BENCHMARKS=(ow_jv_hw  ow_jv_fh  ow_jv_cl  ow_jv_hr  ow_jv_vp)
OW_JV_BENCHMARKS=(ow_jv_hw)
OW_PY_BENCHMARKS=(ow_py_hw  ow_py_ms  ow_py_bf  ow_py_pr  ow_py_dn  ow_py_dh  ow_py_co  ow_py_th  ow_py_vp  ow_py_up)
OW_JS_BENCHMARKS=(ow_js_hw  ow_js_dh  ow_js_th  ow_js_up)
#OW_BENCHMARKS=("${OW_JV_BENCHMARKS[@]}" "${OW_PY_BENCHMARKS[@]}" "${OW_JS_BENCHMARKS[@]}")
OW_BENCHMARKS=("${OW_JV_BENCHMARKS[@]}")

#KN_JV_BENCHMARKS=(kn_jv_hw  kn_jv_fh  kn_jv_cl  kn_jv_hr  kn_jv_vp)
KN_JV_BENCHMARKS=(kn_jv_hw)
KN_PY_BENCHMARKS=(kn_py_hw  kn_py_ms  kn_py_bf  kn_py_pr  kn_py_dn  kn_py_dh  kn_py_co  kn_py_th  kn_py_vp  kn_py_up)
KN_JS_BENCHMARKS=(kn_js_hw  kn_js_dh  kn_js_th  kn_js_up)
#KN_BENCHMARKS=("${KN_JV_BENCHMARKS[@]}" "${KN_PY_BENCHMARKS[@]}" "${KN_JS_BENCHMARKS[@]}")
KN_BENCHMARKS=("${KN_JV_BENCHMARKS[@]}")

GH_JV_BENCHMARKS=(gh_jv_hw)
GH_BENCHMARKS=("${GH_JV_BENCHMARKS[@]}")

# Hydra benchmarks.
declare -A BENCHMARK_CODE
BENCHMARK_CODE[hy_jv_hw]="$DATA_ADDRESS/apps/hy-jv-hello-world.so"
BENCHMARK_CODE[hy_jv_fh]="$DATA_ADDRESS/apps/hy-jv-file-hashing.so"
BENCHMARK_CODE[hy_jv_cl]="$DATA_ADDRESS/apps/hy-jv-classify.zip"
BENCHMARK_CODE[hy_jv_hr]="$DATA_ADDRESS/apps/hy-jv-httprequest.so"
BENCHMARK_CODE[hy_jv_vp]="$DATA_ADDRESS/apps/hy-jv-video-processing.so"

BENCHMARK_CODE[hy_js_hw]="$DATA_ADDRESS/apps/hy-js-hello-world.so"
BENCHMARK_CODE[hy_js_dh]="$DATA_ADDRESS/apps/hy-js-dynamic-html.so"
BENCHMARK_CODE[hy_js_th]="$DATA_ADDRESS/apps/hy-js-thumbnail.zip"
BENCHMARK_CODE[hy_js_up]="$DATA_ADDRESS/apps/hy-js-uploader.so"

BENCHMARK_CODE[hy_py_hw]="$DATA_ADDRESS/apps/hy-py-hello-world.so"
BENCHMARK_CODE[hy_py_ms]="$DATA_ADDRESS/apps/hy-py-mst.so"
BENCHMARK_CODE[hy_py_bf]="$DATA_ADDRESS/apps/hy-py-bfs.so"
BENCHMARK_CODE[hy_py_pr]="$DATA_ADDRESS/apps/hy-py-pagerank.so"
BENCHMARK_CODE[hy_py_dn]="$DATA_ADDRESS/apps/hy-py-dna.so"
BENCHMARK_CODE[hy_py_dh]="$DATA_ADDRESS/apps/hy-py-dynamic-html.so"
BENCHMARK_CODE[hy_py_co]="$DATA_ADDRESS/apps/hy-py-compression.so"
BENCHMARK_CODE[hy_py_th]="$DATA_ADDRESS/apps/hy-py-thumbnail.so"
BENCHMARK_CODE[hy_py_vp]="$DATA_ADDRESS/apps/hy-py-video-processing.so"
BENCHMARK_CODE[hy_py_up]="$DATA_ADDRESS/apps/hy-py-uploader.so"


declare -A BENCHMARK_ENTRYPOINTS
BENCHMARK_ENTRYPOINTS[hy_jv_hw]="com.hello_world.HelloWorld"
BENCHMARK_ENTRYPOINTS[hy_jv_fh]="com.filehashing.FileHashing"
BENCHMARK_ENTRYPOINTS[hy_jv_cl]="com.classify.Classify"
BENCHMARK_ENTRYPOINTS[hy_jv_hr]="com.httprequest.HttpRequest"
BENCHMARK_ENTRYPOINTS[hy_jv_vp]="com.videoprocessing.VideoProcessing"

BENCHMARK_ENTRYPOINTS[hy_js_hw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[hy_js_dh]="com.dynamichtml.DynamicHTML"
BENCHMARK_ENTRYPOINTS[hy_js_th]="com.thumbnail.Thumbnail"
BENCHMARK_ENTRYPOINTS[hy_js_up]="com.uploader.Uploader"

BENCHMARK_ENTRYPOINTS[hy_py_hw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[hy_py_ms]="com.mst.MST"
BENCHMARK_ENTRYPOINTS[hy_py_bf]="com.bfs.BFS"
BENCHMARK_ENTRYPOINTS[hy_py_pr]="com.pr.PageRank"
BENCHMARK_ENTRYPOINTS[hy_py_dn]="com.dna.DNA"
BENCHMARK_ENTRYPOINTS[hy_py_dh]="com.dynamichtml.DynamicHTML"
BENCHMARK_ENTRYPOINTS[hy_py_co]="com.compression.Compression"
BENCHMARK_ENTRYPOINTS[hy_py_th]="com.thumbnail.Thumbnail"
BENCHMARK_ENTRYPOINTS[hy_py_vp]="com.videoprocessing.VideoProcessing"
BENCHMARK_ENTRYPOINTS[hy_py_up]="com.uploader.Uploader"


declare -A BENCHMARK_PAYLOADS
BENCHMARK_PAYLOADS[hy_jv_hw]='{}'
BENCHMARK_PAYLOADS[hy_jv_fh]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[hy_jv_cl]='{"model_url":"'$DATA_ADDRESS'/tensorflow_inception_graph.pb","labels_url":"'$DATA_ADDRESS'/imagenet_comp_graph_label_strings.txt","image_url":"'$DATA_ADDRESS'/eagle.jpg"}'
BENCHMARK_PAYLOADS[hy_jv_hr]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[hy_jv_vp]='{"video":"'$DATA_ADDRESS'/video.mp4","ffmpeg":"'$DATA_ADDRESS'/ffmpeg"}'

BENCHMARK_PAYLOADS[hy_js_hw]='{}'
BENCHMARK_PAYLOADS[hy_js_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[hy_js_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[hy_js_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'

BENCHMARK_PAYLOADS[hy_py_hw]='{}'
BENCHMARK_PAYLOADS[hy_py_ms]='{"size":"100"}'
BENCHMARK_PAYLOADS[hy_py_bf]='{"size":"100"}'
BENCHMARK_PAYLOADS[hy_py_pr]='{"size":"10"}'
BENCHMARK_PAYLOADS[hy_py_dn]='{"fasta_url":"'$DATA_ADDRESS'/bacillus_subtilis.fasta"}'
BENCHMARK_PAYLOADS[hy_py_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[hy_py_co]='{"url":"'$DATA_ADDRESS'/video.mp4"}'
BENCHMARK_PAYLOADS[hy_py_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[hy_py_vp]='{"video":"'$DATA_ADDRESS'/video.mp4","ffmpeg":"'$DATA_ADDRESS'/ffmpeg"}'
BENCHMARK_PAYLOADS[hy_py_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'


declare -A BENCHMARK_SANDBOXES
BENCHMARK_SANDBOXES[hy_jv_hw]="isolate"
BENCHMARK_SANDBOXES[hy_jv_fh]="isolate"
BENCHMARK_SANDBOXES[hy_jv_cl]="process"
BENCHMARK_SANDBOXES[hy_jv_hr]="isolate"
BENCHMARK_SANDBOXES[hy_jv_vp]="process"

BENCHMARK_SANDBOXES[hy_js_hw]="snapshot"
BENCHMARK_SANDBOXES[hy_js_dh]="snapshot"
BENCHMARK_SANDBOXES[hy_js_th]="process"
BENCHMARK_SANDBOXES[hy_js_up]="context"

BENCHMARK_SANDBOXES[hy_py_hw]="snapshot"
BENCHMARK_SANDBOXES[hy_py_ms]="snapshot"
BENCHMARK_SANDBOXES[hy_py_bf]="snapshot"
BENCHMARK_SANDBOXES[hy_py_pr]="snapshot"
BENCHMARK_SANDBOXES[hy_py_dn]="snapshot"
BENCHMARK_SANDBOXES[hy_py_dh]="snapshot"
BENCHMARK_SANDBOXES[hy_py_co]="snapshot"
BENCHMARK_SANDBOXES[hy_py_th]="snapshot"
BENCHMARK_SANDBOXES[hy_py_vp]="process"
BENCHMARK_SANDBOXES[hy_py_up]="snapshot"


declare -A BENCHMARK_SVMIDS
BENCHMARK_SVMIDS[hy_jv_hw]=
BENCHMARK_SVMIDS[hy_jv_fh]=
BENCHMARK_SVMIDS[hy_jv_cl]=
BENCHMARK_SVMIDS[hy_jv_hr]=
BENCHMARK_SVMIDS[hy_jv_vp]=

BENCHMARK_SVMIDS[hy_js_hw]="1"
BENCHMARK_SVMIDS[hy_js_dh]="2"
BENCHMARK_SVMIDS[hy_js_th]="3"
BENCHMARK_SVMIDS[hy_js_up]="4"

BENCHMARK_SVMIDS[hy_py_hw]="5"
BENCHMARK_SVMIDS[hy_py_ms]="6"
BENCHMARK_SVMIDS[hy_py_bf]="7"
BENCHMARK_SVMIDS[hy_py_pr]="8"
BENCHMARK_SVMIDS[hy_py_dn]="9"
BENCHMARK_SVMIDS[hy_py_dh]="10"
BENCHMARK_SVMIDS[hy_py_co]="11"
BENCHMARK_SVMIDS[hy_py_th]="12"
BENCHMARK_SVMIDS[hy_py_vp]="13"
BENCHMARK_SVMIDS[hy_py_up]="14"


# OpenWhisk benchmarks.
BENCHMARK_CODE[ow_jv_hw]="$ARGO_HOME/benchmarks/src/java/cr-hello-world/init.json"
BENCHMARK_CODE[ow_jv_fh]="$ARGO_HOME/benchmarks/src/java/cr-file-hashing/init.json"
BENCHMARK_CODE[ow_jv_cl]="$ARGO_HOME/benchmarks/src/java/cr-classify/init.json"
BENCHMARK_CODE[ow_jv_hr]="$ARGO_HOME/benchmarks/src/java/cr-httprequest/init.json"
BENCHMARK_CODE[ow_jv_vp]="$ARGO_HOME/benchmarks/src/java/cr-video-processing/init.json"

BENCHMARK_CODE[ow_js_hw]="$ARGO_HOME/benchmarks/src/javascript/cr-hello-world/init.json"
BENCHMARK_CODE[ow_js_dh]="$ARGO_HOME/benchmarks/src/javascript/cr-dynamic-html/init.json"
BENCHMARK_CODE[ow_js_th]="$ARGO_HOME/benchmarks/src/javascript/cr-thumbnail/init.json"
BENCHMARK_CODE[ow_js_up]="$ARGO_HOME/benchmarks/src/javascript/cr-uploader/init.json"

BENCHMARK_CODE[ow_py_hw]="$ARGO_HOME/benchmarks/src/python/cr-hello-world/init.json"
BENCHMARK_CODE[ow_py_ms]="$ARGO_HOME/benchmarks/src/python/cr-mst/init.json"
BENCHMARK_CODE[ow_py_bf]="$ARGO_HOME/benchmarks/src/python/cr-bfs/init.json"
BENCHMARK_CODE[ow_py_pr]="$ARGO_HOME/benchmarks/src/python/cr-pagerank/init.json"
BENCHMARK_CODE[ow_py_dn]="$ARGO_HOME/benchmarks/src/python/cr-dna/init.json"
BENCHMARK_CODE[ow_py_dh]="$ARGO_HOME/benchmarks/src/python/cr-dynamic-html/init.json"
BENCHMARK_CODE[ow_py_co]="$ARGO_HOME/benchmarks/src/python/cr-compression/init.json"
BENCHMARK_CODE[ow_py_th]="$ARGO_HOME/benchmarks/src/python/cr-thumbnail/init.json"
BENCHMARK_CODE[ow_py_vp]="$ARGO_HOME/benchmarks/src/python/cr-video-processing/init.json"
BENCHMARK_CODE[ow_py_up]="$ARGO_HOME/benchmarks/src/python/cr-uploader/init.json"


for bench in "${OW_BENCHMARKS[@]}"; do
    BENCHMARK_ENTRYPOINTS["$bench"]="irrelevant"
done


BENCHMARK_PAYLOADS[ow_jv_hw]='{"name":"rbruno"}'
BENCHMARK_PAYLOADS[ow_jv_fh]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[ow_jv_cl]='{"model_url":"'$DATA_ADDRESS'/tensorflow_inception_graph.pb","labels_url":"'$DATA_ADDRESS'/imagenet_comp_graph_label_strings.txt","image_url":"'$DATA_ADDRESS'/eagle.jpg"}'
BENCHMARK_PAYLOADS[ow_jv_hr]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[ow_jv_vp]='{"video_url":"'$DATA_ADDRESS'/video.mp4","ffmpeg_url":"'$DATA_ADDRESS'/ffmpeg"}'

BENCHMARK_PAYLOADS[ow_js_hw]='{"name":"rbruno"}'
BENCHMARK_PAYLOADS[ow_js_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[ow_js_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[ow_js_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'

BENCHMARK_PAYLOADS[ow_py_hw]='{"name":"rbruno"}'
BENCHMARK_PAYLOADS[ow_py_ms]='{"size":"100"}'
BENCHMARK_PAYLOADS[ow_py_bf]='{"size":"100"}'
BENCHMARK_PAYLOADS[ow_py_pr]='{"size":"100"}'
BENCHMARK_PAYLOADS[ow_py_dn]='{"fasta_url":"'$DATA_ADDRESS'/bacillus_subtilis.fasta"}'
BENCHMARK_PAYLOADS[ow_py_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[ow_py_co]='{"url":"'$DATA_ADDRESS'/video.mp4"}'
BENCHMARK_PAYLOADS[ow_py_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[ow_py_vp]='{"video_url":"'$DATA_ADDRESS'/video.mp4","ffmpeg_url":"'$DATA_ADDRESS'/ffmpeg"}'
BENCHMARK_PAYLOADS[ow_py_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'


# Knative benchmarks.
BENCHMARK_CODE[kn_jv_hw]="knative-jv/kn-hello-world"
BENCHMARK_CODE[kn_jv_fh]="knative-jv/kn-file-hashing"
BENCHMARK_CODE[kn_jv_cl]="knative-jv/kn-classify"
BENCHMARK_CODE[kn_jv_hr]="knative-jv/kn-httprequest"
BENCHMARK_CODE[kn_jv_vp]="knative-jv/kn-video-processing"

BENCHMARK_CODE[kn_js_hw]="knative-js/kn-hello-world"
BENCHMARK_CODE[kn_js_dh]="knative-js/kn-dynamic-html"
BENCHMARK_CODE[kn_js_th]="knative-js/kn-thumbnail"
BENCHMARK_CODE[kn_js_up]="knative-js/kn-uploader"

BENCHMARK_CODE[kn_py_hw]="knative-py/kn-hello-world"
BENCHMARK_CODE[kn_py_ms]="knative-py/kn-mst"
BENCHMARK_CODE[kn_py_bf]="knative-py/kn-bfs"
BENCHMARK_CODE[kn_py_pr]="knative-py/kn-pagerank"
BENCHMARK_CODE[kn_py_dn]="knative-py/kn-dna"
BENCHMARK_CODE[kn_py_dh]="knative-py/kn-dynamic-html"
BENCHMARK_CODE[kn_py_co]="knative-py/kn-compression"
BENCHMARK_CODE[kn_py_th]="knative-py/kn-thumbnail"
BENCHMARK_CODE[kn_py_vp]="knative-py/kn-video-processing"
BENCHMARK_CODE[kn_py_up]="knative-py/kn-uploader"


for bench in "${KN_BENCHMARKS[@]}"; do
    BENCHMARK_ENTRYPOINTS["$bench"]="irrelevant"
done


BENCHMARK_PAYLOADS[kn_jv_hw]='{"name":"rbruno"}'
BENCHMARK_PAYLOADS[kn_jv_fh]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[kn_jv_cl]='{"model_url":"'$DATA_ADDRESS'/tensorflow_inception_graph.pb","labels_url":"'$DATA_ADDRESS'/imagenet_comp_graph_label_strings.txt","image_url":"'$DATA_ADDRESS'/eagle.jpg"}'
BENCHMARK_PAYLOADS[kn_jv_hr]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[kn_jv_vp]='{"video":"'$DATA_ADDRESS'/video.mp4","ffmpeg":"'$DATA_ADDRESS'/ffmpeg"}'

BENCHMARK_PAYLOADS[kn_js_hw]='{}'
BENCHMARK_PAYLOADS[kn_js_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[kn_js_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[kn_js_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'

BENCHMARK_PAYLOADS[kn_py_hw]='{}'
BENCHMARK_PAYLOADS[kn_py_ms]='{"size":"100"}'
BENCHMARK_PAYLOADS[kn_py_bf]='{"size":"100"}'
BENCHMARK_PAYLOADS[kn_py_pr]='{"size":"100"}'
BENCHMARK_PAYLOADS[kn_py_dn]='{"fasta_url":"'$DATA_ADDRESS'/bacillus_subtilis.fasta"}'
BENCHMARK_PAYLOADS[kn_py_dh]='{"url":"'$DATA_ADDRESS'/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[kn_py_co]='{"url":"'$DATA_ADDRESS'/video.mp4"}'
BENCHMARK_PAYLOADS[kn_py_th]='{"url":"'$DATA_ADDRESS'/snap.png"}'
BENCHMARK_PAYLOADS[kn_py_vp]='{"video":"'$DATA_ADDRESS'/video.mp4","ffmpeg":"'$DATA_ADDRESS'/ffmpeg"}'
BENCHMARK_PAYLOADS[kn_py_up]='{"url":"'$DATA_ADDRESS'/snap.png"}'


# GraalOS (GraalHost) benchmarks.
BENCHMARK_CODE[gh_jv_hw]="$GRAALOS_SEBS/serverless-benchmarks-java/hello-world/app"

for bench in "${GH_BENCHMARKS[@]}"; do
    BENCHMARK_ENTRYPOINTS["$bench"]="irrelevant"
done

BENCHMARK_PAYLOADS[gh_jv_hw]='{}'
