# Modelos serializados pelo kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep,includedescriptorclasses class br.com.escolamais.app.data.**$$serializer { *; }
-keepclassmembers class br.com.escolamais.app.data.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
# Retrofit: interfaces com funções suspensas
-keep,allowobfuscation interface br.com.escolamais.app.data.EscolaApi
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
