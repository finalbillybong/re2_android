/* Original converted assets, read from the APK (or nitrofiles in host tests). */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "assets.h"
#ifdef __ANDROID__
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <jni.h>
static AAssetManager *manager;
static jobject manager_ref;
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_configure(JNIEnv *env,jclass cls,jobject assets) {
    (void)cls;
    if(manager_ref) (*env)->DeleteGlobalRef(env,manager_ref);
    manager_ref=(*env)->NewGlobalRef(env,assets);
    manager=AAssetManager_fromJava(env,assets);
}
#endif
void *asset_load(const char *path,uint32_t *size_out) {
    if(size_out) *size_out=0;
    if(strncmp(path,"nitro:/",7)) return NULL;
    char name[256];
#ifdef __ANDROID__
    if(!manager) return NULL;
    snprintf(name,sizeof name,"re2/%s",path+7);
    AAsset *asset=AAssetManager_open(manager,name,AASSET_MODE_BUFFER);
    if(!asset) return NULL;
    size_t n=AAsset_getLength(asset);
    void *data=n?malloc(n):NULL;
    if(data && AAsset_read(asset,data,n)!=(int)n) { free(data); data=NULL; }
    AAsset_close(asset);
#else
    snprintf(name,sizeof name,"nitrofiles/%s",path+7);
    FILE *f=fopen(name,"rb");
    if(!f&&!strncmp(path,"nitro:/voice/",13)){snprintf(name,sizeof name,"build/android-assets/re2/%s",path+7);f=fopen(name,"rb");}
    if(!f) return NULL;
    fseek(f,0,SEEK_END);long length=ftell(f);rewind(f);
    size_t n=length>0?(size_t)length:0;
    void *data=n?malloc(n):NULL;
    if(data && fread(data,1,n,f)!=n) {free(data);data=NULL;}
    fclose(f);
#endif
    if(data&&size_out) *size_out=(uint32_t)n;
    return data;
}
void asset_free(void *data) { free(data); }
static int fits(uint32_t at,size_t bytes,uint32_t size) {return at<=size&&bytes<=size-at;}
int model_load(Model *m,const char *path) {
    memset(m,0,sizeof *m);
    uint32_t size;uint8_t *data=asset_load(path,&size);
    if(!data) return 0;
    if(size<sizeof(ModelHeader)) goto invalid;
    const ModelHeader *h=(const ModelHeader *)data;
    size_t frame_size=sizeof(ModelFrame)+6*h->num_bones;
    if(h->magic!=MODEL_MAGIC||h->num_bones>32||h->num_pages>4||
       (h->num_pages&&!fits(h->off_bones,sizeof(ModelBone)*h->num_bones,size))||
       !fits(h->off_lists,4u*h->num_bones*h->num_pages,size)||
       !fits(h->off_frames,frame_size*h->num_frames,size)||
       !fits(h->off_anims,sizeof(ModelAnim)*h->num_anims,size)||
       (h->off_bones&1)||(h->off_lists&3)||(h->off_frames&1)||(h->off_anims&1)) goto invalid;
    m->data=data;m->header=h;m->frame_size=frame_size;
    // RBJ animation-only files deliberately have no skeleton/display lists.
    // Their bone count describes rotations; the actor supplies its own skeleton.
    if(!h->num_pages&&(h->off_bones!=h->off_frames||h->off_lists!=h->off_frames))goto invalid;
    m->bones=h->num_pages?(const ModelBone *)(data+h->off_bones):NULL;
    m->lists=(const uint32_t *)(data+h->off_lists);
    m->anims=(const ModelAnim *)(data+h->off_anims);
    m->anim_pool=(const uint16_t *)(m->anims+h->num_anims);
    size_t pool_at=(const uint8_t *)m->anim_pool-data;
    for(int i=0;i<h->num_anims;i++) {
        const ModelAnim *a=&m->anims[i];
        if(!fits(pool_at,2u*((size_t)a->first+a->count),size)) goto invalid;
        for(int k=0;k<a->count;k++) if(ANIM_FRAME(m->anim_pool[a->first+k])>=h->num_frames) goto invalid;
    }
    // GX cost decoding reads length-prefixed display lists even in headless mode.
    for(int i=0;i<h->num_bones*h->num_pages;i++) {
        uint32_t off=m->lists[i];if(!off) continue;
        if((off&3)||!fits(off,4,size)) goto invalid;
        uint32_t words=*(const uint32_t *)(data+off);
        if(!fits(off+4,(size_t)words*4,size)) goto invalid;
    }
    for(int i=0;h->num_pages&&i<h->num_bones;i++) if(m->bones[i].parent>=i) goto invalid;
    return 1;
invalid:
    free(data);memset(m,0,sizeof *m);return 0;
}
