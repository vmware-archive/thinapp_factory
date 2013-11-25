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

/*
 * -----------------------------------------------------------------------------
 * setproxy.cpp
 *
 *    Provides a command line tool that can be launched by vmrun during manual
 *    mode conversions to apply up-to-date appliance-side proxy settings in the
 *    Windows conversion guest.
 * -----------------------------------------------------------------------------
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <windows.h>
#include <ws2tcpip.h>
#include <wininet.h>
#include <tchar.h>

/*
 * MinGW32's wininet.h is incomplete.
 * Extra definitions borrowed from MinGW64 here:
 * http://sourceforge.net/apps/trac/mingw-w64/browser/experimental/headers_additions_test/include/wininet.h
 */
#ifdef __MINGW32__
typedef struct {
  DWORD dwOption;
  union {
    DWORD dwValue;
    LPSTR pszValue;
    FILETIME ftValue;
  } Value;
} INTERNET_PER_CONN_OPTIONA,*LPINTERNET_PER_CONN_OPTIONA;

typedef struct {
  DWORD dwOption;
  union {
    DWORD dwValue;
    LPWSTR pszValue;
    FILETIME ftValue;
  } Value;
} INTERNET_PER_CONN_OPTIONW,*LPINTERNET_PER_CONN_OPTIONW;

#ifdef UNICODE
#define INTERNET_PER_CONN_OPTION INTERNET_PER_CONN_OPTIONW
#else
#define INTERNET_PER_CONN_OPTION INTERNET_PER_CONN_OPTIONA
#endif

typedef struct {
  DWORD dwSize;
  LPSTR pszConnection;
  DWORD dwOptionCount;
  DWORD dwOptionError;
  LPINTERNET_PER_CONN_OPTIONA pOptions;
} INTERNET_PER_CONN_OPTION_LISTA,*LPINTERNET_PER_CONN_OPTION_LISTA;

typedef struct {
  DWORD dwSize;
  LPWSTR pszConnection;
  DWORD dwOptionCount;
  DWORD dwOptionError;
  LPINTERNET_PER_CONN_OPTIONW pOptions;
} INTERNET_PER_CONN_OPTION_LISTW,*LPINTERNET_PER_CONN_OPTION_LISTW;

#ifdef UNICODE
#define INTERNET_PER_CONN_OPTION_LIST INTERNET_PER_CONN_OPTION_LISTW
#else
#define INTERNET_PER_CONN_OPTION_LIST INTERNET_PER_CONN_OPTION_LISTA
#endif

#define INTERNET_PER_CONN_FLAGS 1
#define INTERNET_PER_CONN_PROXY_SERVER 2
#define INTERNET_PER_CONN_PROXY_BYPASS 3
#define INTERNET_PER_CONN_AUTOCONFIG_URL 4
#define INTERNET_PER_CONN_AUTODISCOVERY_FLAGS 5
#define INTERNET_PER_CONN_AUTOCONFIG_SECONDARY_URL 6
#define INTERNET_PER_CONN_AUTOCONFIG_RELOAD_DELAY_MINS 7
#define INTERNET_PER_CONN_AUTOCONFIG_LAST_DETECT_TIME 8
#define INTERNET_PER_CONN_AUTOCONFIG_LAST_DETECT_URL 9
#define INTERNET_PER_CONN_FLAGS_UI 10

#define INTERNET_OPTION_PER_CONNECTION_OPTION 75

#define PROXY_TYPE_DIRECT 0x00000001
#define PROXY_TYPE_PROXY 0x00000002
#define PROXY_TYPE_AUTO_PROXY_URL 0x00000004
#define PROXY_TYPE_AUTO_DETECT 0x00000008

#define AUTO_PROXY_FLAG_USER_SET 0x00000001
#define AUTO_PROXY_FLAG_ALWAYS_DETECT 0x00000002
#define AUTO_PROXY_FLAG_DETECTION_RUN 0x00000004
#define AUTO_PROXY_FLAG_MIGRATED 0x00000008
#define AUTO_PROXY_FLAG_DONT_CACHE_PROXY_RESULT 0x00000010
#define AUTO_PROXY_FLAG_CACHE_INIT_RUN 0x00000020
#define AUTO_PROXY_FLAG_DETECTION_SUSPECT 0x00000040

#endif /* __MINGW32__ */

#define HELP_STRING \
L"Syntax:\n" \
L"\tsetproxy --none\n" \
L"\tsetproxy --auto\n" \
L"\tsetproxy --pac <pac-url>\n" \
L"\tsetproxy --http <proxy-host> <proxy-port> <no-proxy,no-proxy>\n" \
L"\tsetproxy --socks <proxy-host> <proxy-port> <no-proxy,no-proxy>\n" \
L"\tsetproxy --add-exception <no-proxy>\n" \
L"\tsetproxy --del-exception <no-proxy>\n"

void
usage(void)
{
   _putws(HELP_STRING);
   return;
}

int
_tmain(int argc, _TCHAR *argv[])
{
   enum {
      PROXY_NONE,
      PROXY_WPAD,
      PROXY_SOCKS,
      PROXY_HTTP,
      PROXY_PAC,
      ADD_EXCEPTION,
      DEL_EXCEPTION,
   } actionMode;

   LPWSTR pacUrl = NULL;
   LPWSTR proxyBypass = NULL;
   LPWSTR modifyBypass = NULL;
   LPWSTR token = NULL;
   LPWSTR proxyHost = NULL;
   LPWSTR proxyPort = NULL;
   LPWSTR temp = NULL;
   LPCWSTR prefix = NULL;

   size_t len = 0;

   HRESULT hr;
   INTERNET_PER_CONN_OPTION_LIST optionList;
   INTERNET_PER_CONN_OPTION option[5];
   unsigned int nOptions = 0;
   DWORD nSize = sizeof(INTERNET_PER_CONN_OPTION_LIST);
   int bypassSize = 32;
   int bypassCount = 0;
   int i;
   LPWSTR *bypassList = (LPWSTR *)malloc(bypassSize * sizeof(LPWSTR));

   optionList.dwSize = nSize;
   optionList.pszConnection = NULL;
   optionList.dwOptionError = 0;
   optionList.pOptions = option;

   /* Argument 1 defines the mode */
   if (argc >= 2) {
      if (wcscmp(argv[1], L"--none") == 0) {
         actionMode = PROXY_NONE;
      } else if (wcscmp(argv[1], L"--auto") == 0) {
         actionMode = PROXY_WPAD;
      } else if (wcscmp(argv[1], L"--socks") == 0) {
         actionMode = PROXY_SOCKS;
      } else if (wcscmp(argv[1], L"--http") == 0) {
         actionMode = PROXY_HTTP;
      } else if (wcscmp(argv[1], L"--pac") == 0) {
         actionMode = PROXY_PAC;
      } else if (wcscmp(argv[1], L"--add-exception") == 0) {
         actionMode = ADD_EXCEPTION;
      } else if (wcscmp(argv[1], L"--del-exception") == 0) {
         actionMode = DEL_EXCEPTION;
      } else {
         fwprintf(stderr, L"Unrecognized mode %s\n", argv[1]);
         usage();
         return 1;
      }
   } else {
      usage();
      return 1;
   }

   // Stage 1: data input
   switch (actionMode) {
   case ADD_EXCEPTION:
   case DEL_EXCEPTION:
      if (argc < 3) {
         fwprintf(stderr, L"Expected 2 arguments\n");
         usage();
         return 1;
      }

      modifyBypass = argv[2];

      break;

   case PROXY_NONE:
   case PROXY_WPAD:
      /* Nothing to do here */
      break;

   case PROXY_HTTP:
   case PROXY_SOCKS:
      if (argc < 5) {
         fwprintf(stderr, L"Expected 4 arguments\n");
         usage();
         return 1;
      }

      proxyHost = argv[2];
      proxyPort = argv[3];
      proxyBypass = argv[4];

      break;

   case PROXY_PAC:
      pacUrl = argv[2];
      break;
   }

   // Stage 2: Build settings structure
   switch (actionMode) {
   case ADD_EXCEPTION:
   case DEL_EXCEPTION:
      nOptions = 1;
      option[0].dwOption = INTERNET_PER_CONN_PROXY_BYPASS;
      optionList.dwOptionCount = nOptions;

      if (!InternetQueryOption(NULL, INTERNET_OPTION_PER_CONNECTION_OPTION, &optionList, &nSize)) {
         fwprintf(stderr, L"Cannot get current proxy bypass err=%u\n", GetLastError());
         return 1;
      }

      /* Reset nSize */
      nSize = sizeof(INTERNET_PER_CONN_OPTION_LIST);

      /*
       * Go through the list of exceptions to see if the one we're trying to
       * put in/remove is already there.
       */
      if (option[0].Value.pszValue == NULL) {
         if (actionMode == ADD_EXCEPTION) {
            option[0].Value.pszValue = modifyBypass;
            break;
         } else if (actionMode == DEL_EXCEPTION) {
            /* Nothing to handle for DEL_EXCEPTION on an empty list. */
            fwprintf(stderr, L"No exceptions to delete, skipping.");
            return 0;
         }
      }

      token = wcstok(option[0].Value.pszValue, L";");
      if (!token) {
         fwprintf(stderr, L"Unexpected token\n");
         return 1;
      }

      do {
         if (wcscmp(token, modifyBypass) == 0) {
            if (actionMode == ADD_EXCEPTION) {
               _putws(L"Requested bypass already exists; exiting.");
               return 0;
            } else if (actionMode == DEL_EXCEPTION) {
               _putws(L"Removed bypass.");
               /* Not adding to the bypassList here */
            }
         } else {
            bypassList[bypassCount++] = wcsdup(token);
            len += wcslen(token) + 1; /* semicolon */
         }
      } while ((token = wcstok(NULL, L";")) != NULL);

      if (actionMode == ADD_EXCEPTION) {
         /* If we're here the exception isn't already in the list so add it. */
         bypassList[bypassCount++] = modifyBypass;
      }

      GlobalFree(option[0].Value.pszValue);
      option[0].Value.pszValue = (LPWSTR)malloc(sizeof(WCHAR) * (len + 1));
      option[0].Value.pszValue[0] = L'\0';

      /* Join the strings back together */
      for (i = 0; i < bypassCount; i++) {
         wcsncat(option[0].Value.pszValue, bypassList[i], len);
         if (i != bypassCount - 1) {
            wcsncat(option[0].Value.pszValue, L";", len);
         }
      }

      break;

   case PROXY_NONE:
      // None of the other options matter if this is set.
      nOptions = 1;

      _putws(L"Unsetting all proxy settings");

      option[0].dwOption = INTERNET_PER_CONN_FLAGS;
      option[0].Value.dwValue = PROXY_TYPE_DIRECT;

      break;

   case PROXY_WPAD:
      nOptions = 1;

      _putws(L"Setting proxy settings to auto detect");

      option[0].dwOption = INTERNET_PER_CONN_FLAGS;
      option[0].Value.dwValue = PROXY_TYPE_AUTO_DETECT;

      break;

   case PROXY_SOCKS:
   case PROXY_HTTP:
      nOptions = 3;

      wprintf(L"Setting proxy to %s:%s\n", proxyHost, proxyPort);

      // Not exactly sure what the correct syntax is but this seems to work
      prefix = (actionMode == PROXY_SOCKS) ? L"socks=" : L"http://";
      len = wcslen(prefix) + wcslen(proxyHost) + wcslen(proxyPort) + 2;

      temp = (LPWSTR)malloc(len * sizeof(WCHAR));
      hr = _snwprintf(temp, len, L"%s%s:%s", prefix, proxyHost, proxyPort);
      temp[len - 1] = L'\0';

      if (FAILED(hr)) {
         fwprintf(stderr, L"Failed to build proxy setting string.\n");
         return 1;
      }

      option[0].dwOption = INTERNET_PER_CONN_FLAGS;
      option[0].Value.dwValue = PROXY_TYPE_PROXY;

      option[1].dwOption = INTERNET_PER_CONN_PROXY_SERVER;
      option[1].Value.pszValue = temp;

      option[2].dwOption = INTERNET_PER_CONN_PROXY_BYPASS;
      option[2].Value.pszValue = proxyBypass;

      break;

   case PROXY_PAC:
      nOptions = 2;

      wprintf(L"Setting proxy to PAC URL %s\n", pacUrl);

      option[0].dwOption = INTERNET_PER_CONN_FLAGS;
      option[0].Value.dwValue = PROXY_TYPE_AUTO_PROXY_URL;

      option[1].dwOption = INTERNET_PER_CONN_AUTOCONFIG_URL;
      option[1].Value.pszValue = pacUrl;

      break;
   }

   optionList.dwOptionCount = nOptions;

   if (!InternetSetOption(NULL, INTERNET_OPTION_PER_CONNECTION_OPTION, &optionList, nSize)) {
      fwprintf(stderr, L"Can't set proxy options (%d). Continuing anyway.",
               GetLastError());
   } else {
      // Refresh the settings
      InternetSetOption(NULL, INTERNET_OPTION_REFRESH, NULL, NULL);
   }

   if (temp) {
      free(temp);
   }

   return 0;
}
