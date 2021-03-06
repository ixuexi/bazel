// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.skyframe;

import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.packages.BuildFileContainsErrorsException;
import com.google.devtools.build.lib.packages.BuildFileNotFoundException;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.util.Preconditions;
import com.google.devtools.build.skyframe.LegacySkyKey;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import javax.annotation.Nullable;

/**
 * SkyFunction that throws a {@link BuildFileContainsErrorsException} for {@link Package} that
 * loaded, but was in error. Must only be requested when a SkyFunction wishes to ignore the errors
 * in a {@link Package} in keep_going mode, but to shut down the build in nokeep_going mode. Thus,
 * this SkyFunction should only be requested when the corresponding {@link PackageFunction} has
 * already been successfully called and the resulting Package contains an error.
 *
 * <p>This SkyFunction never returns a value, only throws a {@link BuildFileNotFoundException}, and
 * should never return null, since all of its dependencies should already be present.
 */
public class PackageErrorFunction implements SkyFunction {
  public static SkyKey key(PackageIdentifier packageIdentifier) {
    return LegacySkyKey.create(SkyFunctions.PACKAGE_ERROR, packageIdentifier);
  }

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws PackageErrorFunctionException, InterruptedException {
    PackageIdentifier packageIdentifier = (PackageIdentifier) skyKey.argument();
    try {
      SkyKey packageKey = PackageValue.key(packageIdentifier);
      // Callers must have tried to load the package already and gotten the package successfully.
      Package pkg =
          ((PackageValue) env.getValueOrThrow(packageKey, NoSuchPackageException.class))
              .getPackage();
      Preconditions.checkState(pkg.containsErrors(), skyKey);
      throw new PackageErrorFunctionException(
          new BuildFileContainsErrorsException(packageIdentifier), Transience.PERSISTENT);
    } catch (NoSuchPackageException e) {
      throw new IllegalStateException(
          "Function should not have been called on package with exception", e);
    }
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static class PackageErrorFunctionException extends SkyFunctionException {
    public PackageErrorFunctionException(
        BuildFileContainsErrorsException cause, Transience transience) {
      super(cause, transience);
    }
  }
}
