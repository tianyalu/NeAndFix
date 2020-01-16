# NeAndFix 阿里云AndFix热修复原理分析与实现  
说明：本文所示代码在Android5.1的模拟器上运行成功了，但是基于`AndFix`本身的限制，在我的米6 8.0真机上未运行成功。  
## 1.1 AndFix热修复原理分析
`AndFix` GitHub地址：[https://github.com/alibaba/AndFix](https://github.com/alibaba/AndFix)  
The implementation principle of AndFix is method body's replacing:  
![image](https://github.com/tianyalu/NeAndFix/raw/master/show/principle.png)  

热修复整体流程：  
![image](https://github.com/tianyalu/NeAndFix/raw/master/show/process.png)   


## 1.2 Android源码跟踪分析验证

源码在线查看：[http://androidxref.com/](http://androidxref.com/)

代码流程如下：  
> 1. [Android 5.1.1版本中的ArtMethod结构体路径：art/runtime/mirror/art_method.h#560](http://androidxref.com/5.1.1_r6/xref/art/runtime/mirror/art_method.h#560)  
> 2. [/frameworks/base/core/jni/AndroidRuntime.cpp#951](http://androidxref.com/5.1.1_r6/xref/frameworks/base/core/jni/AndroidRuntime.cpp#951)  
> 3. [/art/runtime/jni_internal.cc#FindClass()#599](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2117)  
> 4. [/art/runtime/class_linker.cc#DefineClass()#2218](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2218/)  
> 5. [/art/runtime/class_linker.cc#LoadClass()#2727](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2727)  
> 6. [/art/runtime/class_linker.cc#LoadClassMembers()#2767](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2767)  
> 7. [/art/runtime/class_linker.cc#2627_LinkCode()#2627](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2627)  
> 8. [#2643_LinkMethod()/art/runtime/oat_file.cc#595](http://androidxref.com/5.1.1_r6/xref/art/runtime/oat_file.cc#595)  
> 9. [/art/runtime/oat_file.h#GetPortableCode()#100](http://androidxref.com/5.1.1_r6/xref/art/runtime/oat_file.h#100)  
> 10. [/art/runtime/oat_file.h#GetQuickCode()#109](http://androidxref.com/5.1.1_r6/xref/art/runtime/oat_file.h#109)  
> 11. [#2647_NeedsInterpreter()/art/runtime/class_linker.cc#2525](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2525)  
> 12. [#2705_UnregisterNative()/art/runtime/mirror/art_method.cc#364](http://androidxref.com/5.1.1_r6/xref/art/runtime/mirror/art_method.cc#364)  
> 13. [#2717_UpdateMethodsCode()/art/runtime/instrumentation.cc#679](http://androidxref.com/5.1.1_r6/xref/art/runtime/instrumentation.cc#679)  
> 14. [#2717_UpdateEntrypoints()/art/runtime/instrumentation.cc#85](http://androidxref.com/5.1.1_r6/xref/art/runtime/instrumentation.cc#85)  
> 15. [JNI#GetStaticMethodID/art/runtime/jni_internal.cc#943](http://androidxref.com/5.1.1_r6/xref/art/runtime/jni_internal.cc#943)  
> 16. [JNI#FindMethodID/art/runtime/jni_internal.cc#140](http://androidxref.com/5.1.1_r6/xref/art/runtime/jni_internal.cc#140)  
> 17. [http://androidxref.com/5.1.1_r6/xref/art/runtime/scoped_thread_state_change.h#171](http://androidxref.com/5.1.1_r6/xref/art/runtime/scoped_thread_state_change.h#171)  
> 18. [art/runtime/class_linker.cc#LoadClassMembers()#2767](http://androidxref.com/5.1.1_r6/xref/art/runtime/class_linker.cc#2767)  


## 1.3 核心代码原理实现  
### 1.3.1 名词解释

> Class：类在内存中的一个映射，包括类中包含的instant field, static fields, virtual methods table, direct methods table, interface methods tables等；  
> DexFile: Dex文件在内存中的一个映射，其实就是parse Dex的结果，通过它可以找到DexFile中各个成员的名字和各个段中的偏移；  
> DexCache: 是一个buffer,用来保存已经访问过的fields,methods等；  
> ArtMethod: 保存了任何一个方法的入口（native或者“蹦床”函数）;  
> ArtField: 类似ArtMethod;  
> direct methods: 有一个ArtMethod数组，包含了constructor函数和private函数；  
> ClassLoader: Load当前类的ClassLoader(native层);  

每一个Class包含了一个ifTable数组，一个ifTable对应Class直接implement的一个interface，或者通过supper class间接
implement的一个interface。一个ifTable包含了一个ArtMethod数组，每个ArtMethod对应一个implement的方法，这个方法
可能是当前类实现的，也可以是super类实现的。  
每个Class包含了一个vTable,这个vTable也是一个ArtMethod数组，包含了所有的public方法以及直接父类或者间接父类继承
过来的public方法。  

每个Dex File在内存中对会对应有一个DEX Cache，例如蘑菇街APP有5个multidex，那么虚拟机内存中就会有5个Dex Cache。
Dex Cache中保存了对应的DEX中的域（fields），方法（methods），类型（types），字符串（String）。
Dex Cache其实就是一个buffer，用来保存已经解析过的fields，methods，types，String。  

```c++
File:art_method.h

class ArtMethod FINAL {
protected:
...
    struct PACKED(4) PtrSizedFields {   
        // 解释模式时调用此函数的入口
        void* entry_point_from_interpreter_;
        // 此函数是一个JNI方法时的调用入口
        void* entry_point_from_jni_;
        // 以本地代码调用时的函数入口
        void* entry_point_from_quick_compiled_code_;
    }ptr_sized_fields_  
    ...
} 
```
ArtMethod最重要的就是这3个entry_point了，分包表示解释模式时调用此函数的入口，此函数是一个JNI方法时的调用入口，
以本地代码调用时的函数入口。  
在查找方法时，比如A调用B方法，假设A，B在同一个dex中，警告dex2oat优化后，本地代码将会根据B在DEX中的method_id，索引
到DEX_Cache中的resolved_method，然后调用B方法的ArtMethod的entry_point_from_quick_compiled_code指针指向的
方法入口。  

可以参考：[ART运行时之method/field加载](https://blog.csdn.net/guoguodaern/article/details/60878829)  

### 1.3.2 流程
> 要执行A类中的B方法：
>
> 1，加载A类
>
> 2，获取B方法的ArtMethod
>
> 3，执行B方法


DirectMethod： 静态成员、私有成员、构造
VirtualMethod：虚成员函数


### 1.3.3 热修复文件生成DEX文件
参考：[Mac命令行生成DEX文件](https://github.com/tianyalu/github-doc/blob/master/android/mac_generate_dex.md)


## 1.4 扩展：突破兼容性问题解决方案

兼容性问题带来的思考：
是不是可以替换整个ArtMethod结构体？
memcpy(bugArtMethod, fixArtMethod,  size(ArtMethod));
如何动态获取各个版本的ArtMethod的大小？
数组：连续的内存空间，ArtMethod在 内存上是连续紧密排列。


> **解决方案：**
>
> **获取相邻ArtMethod结构体的地址的偏移量来动态获取ArtMethod结构体的大小！**



Class ArtMethodStruct{

​	a();

​	b();

}