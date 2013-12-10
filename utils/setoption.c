/* ***********************************************************************
 * VMware ThinApp Factory
 * Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ***********************************************************************/

#include <windows.h>
#include <stdio.h>
#include <tchar.h>

int wmain(int argc, wchar_t* argv[])
{
   if (argc!=5)
      wprintf(L"Usage: %ls Section Key Value IniFilename\n"
              L"   example: %ls BuildOptions QualityReportingEnabled 1 .\\package.ini\n",
             argv[0], argv[0]);
   else
   {
      WCHAR *Value=argv[3];
      if (Value[0]==0) Value=NULL;
      if (!WritePrivateProfileString(argv[1], argv[2], Value, argv[4]))
      {
         DWORD ErrCode=GetLastError();
         wprintf(L"WritePrivateProfileString failed with error code %lu\n"
                 L"\"%ls\" \"%ls\" \"%ls\" \"%ls\"\n",
                 ErrCode,
                 argv[1], argv[2], argv[3], argv[4]);
      }
      else
         printf("Set [%ls] %ls=%ls for %ls\n", argv[1], argv[2], Value ? Value : L"NULL", argv[4]);
   }

   return 0;
}
