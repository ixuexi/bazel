// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <type_traits>  // static_assert

#include "src/main/native/windows/jni-util.h"

namespace bazel {
namespace windows {

using std::string;
using std::wstring;

// Ensure we can safely cast (const) jchar* to (const) WCHAR* and LP(C)WSTR.
// This is true with MSVC but not always with GCC.
static_assert(sizeof(jchar) == sizeof(WCHAR),
              "jchar and WCHAR should be the same size");

string GetJavaUTFString(JNIEnv* env, jstring str) {
  string result;
  if (str != nullptr) {
    const char* jstr = env->GetStringUTFChars(str, nullptr);
    result.assign(jstr);
    env->ReleaseStringUTFChars(str, jstr);
  }
  return result;
}

wstring GetJavaWstring(JNIEnv* env, jstring str) {
  wstring result;
  if (str != nullptr) {
    const jchar* jstr = env->GetStringChars(str, nullptr);
    // We can safely reinterpret_cast because of the static_assert checking that
    // sizeof(jchar) = sizeof(WCHAR).
    result.assign(reinterpret_cast<const WCHAR*>(jstr));
    env->ReleaseStringChars(str, jstr);
  }
  return result;
}

}  // namespace windows
}  // namespace bazel
