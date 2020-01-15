#include <jni.h>
#include <string>

#import "art_5_1.h"
/**
 * 底层核心代码
 * 替换ArtMethod结构体中的字段
 * Method>ArtMethod>获取字段
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_sty_ne_andfix_DexManager_replace(JNIEnv *env, jobject thiz, jobject bug_method,
                                          jobject fixed_method) {
    //获取带bug的Method的ArtMethod
    art::mirror::ArtMethod *bugArtMethod = reinterpret_cast<art::mirror::ArtMethod *>(env->FromReflectedMethod(bug_method));
    //获取修复好的Method的ArtMethod
    art::mirror::ArtMethod *fixArtMethod = reinterpret_cast<art::mirror::ArtMethod *>(env->FromReflectedMethod(fixed_method));
    
    reinterpret_cast<art::mirror::Class*>(fixArtMethod->declaring_class_)->class_loader_ =
            reinterpret_cast<art::mirror::Class*>(bugArtMethod->declaring_class_)->class_loader_; //for plugin classloader
    reinterpret_cast<art::mirror::Class*>(fixArtMethod->declaring_class_)->clinit_thread_id_ =
            reinterpret_cast<art::mirror::Class*>(bugArtMethod->declaring_class_)->clinit_thread_id_;
    reinterpret_cast<art::mirror::Class*>(fixArtMethod->declaring_class_)->status_ =
            reinterpret_cast<art::mirror::Class*>(bugArtMethod->declaring_class_)->status_-1;
    //for reflection invoke
    reinterpret_cast<art::mirror::Class*>(fixArtMethod->declaring_class_)->super_class_ = 0;

    bugArtMethod->declaring_class_ = fixArtMethod->declaring_class_;
    bugArtMethod->dex_cache_resolved_types_ = fixArtMethod->dex_cache_resolved_types_;
    bugArtMethod->access_flags_ = fixArtMethod->access_flags_ | 0x0001;
    bugArtMethod->dex_cache_resolved_methods_ = fixArtMethod->dex_cache_resolved_methods_;
    bugArtMethod->dex_code_item_offset_ = fixArtMethod->dex_code_item_offset_;
    bugArtMethod->method_index_ = fixArtMethod->method_index_;
    bugArtMethod->dex_method_index_ = fixArtMethod->dex_method_index_;

    bugArtMethod->ptr_sized_fields_.entry_point_from_interpreter_ =
            fixArtMethod->ptr_sized_fields_.entry_point_from_interpreter_;

    bugArtMethod->ptr_sized_fields_.entry_point_from_jni_ =
            fixArtMethod->ptr_sized_fields_.entry_point_from_jni_;
    bugArtMethod->ptr_sized_fields_.entry_point_from_quick_compiled_code_ =
            fixArtMethod->ptr_sized_fields_.entry_point_from_quick_compiled_code_;
}