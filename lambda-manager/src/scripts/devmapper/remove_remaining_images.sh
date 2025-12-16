function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

cd $(DIR)

sudo pkill firecracker

images=$(ls -l | grep lambda_ | awk '{print $9}' | sed 's/.\{5\}$//')

for i in $images
do
     bash $(DIR)/delete_overlay_image.sh $i $ARGO_HOME/lambda_manager/codebase/$i/hydra.img
done

images=$(sudo dmsetup ls | grep lambda_ | awk '{print $1}')

for i in $images
do
     bash $(DIR)/delete_overlay_image.sh $i $ARGO_HOME/lambda_manager/codebase/$i/hydra.img
done

images=$(ls -la /dev/mapper/ | awk '{print $9}' | tail -n +5)

for i in $images
do
     bash $(DIR)/delete_overlay_image.sh $i $ARGO_HOME/lambda_manager/codebase/$i/hydra.img
done

bash $(DIR)/delete_base_images.sh
