# 검증 내역

대상: 온라디오 0.1.0-dev2 / kr.jinsu.radio.dev / versionCode 2 / minSdk 26 / targetSdk 36.

- Galaxy-Tab-S11-Ultra의 Termux JDK 21, Gradle 8.14.5, AGP 8.9.1, Android SDK 36, native AAPT2로 빌드했다.
- assembleDebug, lintDebug(오류 0건), 기본 단위 테스트와 정렬·편성 HTML 파서 테스트을 통과했다.
- 공식 API를 실제 앱의 RadioData Java 구현으로 호출하여 KBS 5개, MBC 2개, SBS 2개, CBS 2개 채널의 편성 목록과 현재 프로그램을 확인했다. EBS·TBS·YTN을 포함한 전체 14개 채널의 편성 데이터와 현재 프로그램 조회를 실제 앱의 RadioData Java 구현으로 검증했다.
- KBS·MBC·SBS·CBS·EBS 기존 12개 채널의 HLS 목록과 음성 세그먼트 수신을 확인했다. 이것은 실제 Android 디코더에서의 청취 시험과 구분한다.
- 신규 TBS·YTN 2개 채널의 HLS 목록과 실제 음성 세그먼트 수신을 추가 확인했다.
- 하단 재생바의 root 부착 누락을 수정하고, root > header/body/divider/mini의 부모 관계와 body 안의 스크롤 영역을 독립 소스 검토로 확인했다. 목록의 프로그램 정보는 별도 네트워크 작업에서 받아 메인 스레드에서 변경된 문구만 갱신한다.
- 독립 소스 검토에서 Android API 호환성, 오류 컨트롤 상태, 편성 캐시 만료, 백그라운드 방송정보 갱신, 알림 재진입과 편성표 채널 전환의 상태 일치를 점검하고 발견사항을 수정했다.
- ADB 연결 기기가 없어 설치·실제 화면 렌더링·화면 꺼짐 상태의 장시간 청취·블루투스/전화 통화와의 오디오 전환은 미검증이다. 소스·정적 검사·서버 응답 검증을 실기기 성공으로 표시하지 않는다.

인터넷 방송은 실제 FM 전파 수신과 다르며 데이터 통신을 사용한다. 방송사 공식 API 응답에 따라 편성정보를 갱신한다. 스트리밍 주소는 만료에 대비하여 재생 직전에 새로 발급받는다.

정렬은 주파수 오름차순이 기본이며 방송국 이름은 한국어 Collator 기준이다. 동일 sort_order 키로 SharedPreferences 저장·복원을 수행하고 목록/즐겨찾기/이전·다음이 같은 정렬 함수를 사용함을 확인했다.

## 0.1.0-dev3 검증 범위

변경: 헤더 터치 정렬 4개 상태와 저장·복원, 검정 활성 화살표, 편성표 현재 행 중앙 이동. 이전 버전의 스트림·편성 데이터 검증은 같은 RadioData 조회 코드이므로 재사용합니다. UI 규칙 테스트에서 열 전환/방향 반전/활성 열 화살표/행 중앙 좌표/내림차순 순서를 검증합니다. 화면 View 부모 관계와 레이아웃 완료 후 스크롤 위치 적용을 독립 소스 검토합니다. 실기기 화면 검증은 미완료입니다.

블루투스 이어폰 해제 시 재생 중지: Media3 오디오 출력 전환 처리와 Bluetooth A2DP·SCO·LE Audio·보청기 출력 제거 콜백을 함께 적용한다. 마지막 Bluetooth 출력이 제거되면 재생과 스트리밍을 중지하고, 연결 해제 전 시작된 방송 주소 조회가 나중에 완료돼도 스피커로 자동 재생하지 않는다. 재연결만으로 자동 재생하지 않으며 Bluetooth 연결 권한을 새로 요구하지 않는다. 실제 이어폰 연결·해제는 실기기 미검증 항목이다.

재생바 터치 시 재생화면의 상단과 본문을 180ms 동안 48dp 아래에서 위로 부드럽게 전환한다. 하단 재생바는 움직이지 않고, 시스템 애니메이션 비활성 설정을 존중한다. 화면 이동으로 분리된 View의 중앙이동 콜백은 즉시 해제한다. 재생 요청에는 오디오 출력 세대값을 실어 서비스의 실제 play/setPlayWhenReady 시점에도 연결 해제 이전 요청을 거부한다.

DEV3 자동검증: 정렬·중앙 좌표·오디오 출력 정책을 포함한 단위 테스트 20건과 빌드·Android Lint를 대상으로 검증한다. 외부 명시적 재생은 새 스트림 주소와 새 출력 세대를 받고, 이전 앱 요청은 거부한다. 외부 재생 준비 중 pause/stop 또는 재차 BT 해제 시 해당 요청을 취소한다.

## 0.1.0-dev4 검증 범위

재생화면 열기 360ms 및 상단/시스템 뒤로가기 닫기 360ms. header/body만 이동하고 dock는 고정한다. 재구성·종료 시 애니메이션 listener 제거 후 취소하고, 닫는 중 중복 뒤로가기를 무시한다. 시스템 애니메이션 비활성 시 즉시 화면 이동한다. assembleDebug/lintDebug/testDebugUnitTest 및 독립 소스 검토를 진행한다. 기존 API 데이터 경로는 변경하지 않아 이전 네트워크 검증을 재사용한다. 실제 단말 애니메이션 체감은 미검증이다.

독립 검토에서 닫기 도중 채널 변경·즐겨찾기·회전으로 화면 재구성 시 뒤로가기 목적지가 취소되는 문제를 발견했다. build() 시작에서 닫는 중이면 목록 화면을 먼저 확정하도록 수정했다.

## 0.1.0-dev5 검증 범위

기존 48dp 부분 이동 후 화면 교체가 갑자기 사라지는 원인이었다. 재생바 위 영역을 FrameLayout으로 분리하고 header/body를 한 panel로 전체 높이 이동한다. 목록을 Bitmap 배경으로 유지해 이동 중 빈 화면을 방지한다. 600ms AccelerateDecelerate 전환, 투명도 효과 제거, pre-draw 이후 높이 측정, generation으로 예약 콜백 무효화, 재구성/종료 취소를 검토한다. 네트워크/오디오 구현은 변경하지 않는다. 실기기 체감과 렌더링은 미검증이다.

## 0.1.0-dev6 검증 범위

변경은 PLAYER_TRANSITION_MS 600 → 420 및 개발 버전 증가이다. 열기·닫기 공통 상수를 사용하며 이전 전체 높이 이동/목록 위치 복원/애니메이션 취소 로직은 동일하다. 420/600=0.7을 확인한다. 실제 단말 애니메이션 체감은 미검증이다.

## 0.1.0-dev7 검증 범위

MainActivity.step()의 채널 이동 순서를 orderedStations(sortOrder)에서 orderedStations("frequency")로 변경한다. 목록 populate()는 저장된 정렬 기준을 유지한다. 즐겨찾기 필터/빈 목록 fallback/끝에서 순환하는 기존 정책은 유지한다. 재생바 previous/next가 step(-1)/step(1)을 호출하고 실제 play(station)를 수행함을 소스 검토한다.

## 0.1.0-dev8 검증 범위

Media3 1.8 ForwardingSimpleBasePlayer의 State.availableCommands 및 handleSeek를 사용해 단일 미디어 항목에서도 NEXT/PREVIOUS 4개 명령을 일관되게 게시한다. 기존 블루투스 출력 세대 보호 ForwardingPlayer를 보존한다. 앱 버튼도 동일 seek 명령을 사용한다. 서비스 별도 채널 IO·누적 pending ID·취소 ticket·출력 세대, Activity resolver와의 상호 무효화를 검토한다. ChannelNavigation 단위 테스트와 Robolectric 미디어 플레이어 명령/이벤트 검증을 추가한다. 공식 API 참조: https://developer.android.com/reference/androidx/media3/common/ForwardingSimpleBasePlayer 및 https://developer.android.com/media/media3/session/control-playback . 실제 알림/잠금화면 UI 및 블루투스 버튼 물리 조작은 미검증이다.

## 0.1.0-dev9 검증 범위

ProgramProgress는 절대 시작/종료 시각과 현재시각으로 비율을 계산하고 0∼100% 범위로 제한한다. 앱 ProgressView는 seek thumb·player 참조·터치 처리가 없는 그리기 전용이다. RadioSessionPlayer의 표시 상태에 프로그램 길이/시각을 투영하되 실제 ExoPlayer HLS 타임라인은 수정하지 않는다. 시간 탐색 명령을 모두 차단하고 이전/다음 채널 명령은 보존한다. 일시정지 중 wall-clock 경과, 자정 프로그램, 편성정보 누락/만료, 탐색 차단, 터치 무처리와 기존 미디어 세션 명령을 검증한다. 실제 시스템 UI 표시는 실기기 미검증이다.

## 0.1.0-dev10 검증 범위

각 목록 항목의 ProgramTimeRow는 시간 텍스트와 비상호작용 ProgramProgressView로만 구성한다. rowTimeProgress는 화면 재구성/목록 갱신 시 해제·재구성하며 foreground ticker가 현재 편성으로 1초마다 업데이트한다. 유효한 현재 편성이 없으면 줄 전체를 숨긴다. 목록·하단·재생화면에 숫자 퍼센트를 출력하는 경로와 player progressLabel을 제거했다. 하단 정상 재생 문구만 제외하고 연결/일시정지/오류는 보존한다. 좁은 목록 열의 레이아웃·터치 무처리·정보 누락/종료·숫자 퍼센트 제거를 Robolectric에서 검사한다. 실기기 렌더링은 미검증이다.

## 0.1.0-dev11 검증 범위

ProgramTimeRow 시간은 글자에 필요한 폭만 사용하고 bar가 남은 폭 전체를 차지한다. 16%/56dp 폭 제한을 제거한다. 좁은 열에서는 시간 최대 폭을 제한해 최소 16dp bar 공간을 확보한다. 140/240/500dp 폭에서 bar 시작이 시간 바로 뒤이고 끝이 행 끝에 맞으며 가용 폭 증가분 전체가 bar에 반영되는지 검증한다. 실기기 렌더링은 미검증이다.

재생화면 본문의 playerProgress View·필드·갱신 경로를 제거한다. 하단 miniProgress와 목록 ProgramTimeRow의 진행률 표시는 유지한다.

## 0.1.0-dev12 검증 범위

buildPlayer의 ScrollView는 body에서 weight1을 사용하고 편성표/홈페이지 row는 scroll의 형제 footer로 고정한다. 설명과 안내문은 스크롤에 남고 줄 수 제한·말줄임·삭제를 하지 않는다. 재생바는 기존 root 하단에 유지한다. 실제 MainActivity를 네트워크 없는 하위 클래스로 구성한 Robolectric 시험으로 설명 길이·스크롤·360dp 폭 및 560/820dp 높이에서 버튼 위치 고정과 설명 원문 유지, 재생바 비겹침을 검증한다. 실기기 렌더링은 미검증이다.

## 0.1.0-dev13 검증 범위

SwipeLayout은 방향 우세/터치 slop을 넘으면 자식 탭을 취소하고 부모가 처리하며, 48dp 이상 최종 이동 시 1회 동작한다. dock는 위 방향만 열고, player panel은 수평만 기존 step 경로를 사용한다. 다른 화면에서는 player swipe를 비활성화한다. 실제 MotionEvent dispatch와 clickable child/ScrollView, 실제 MainActivity에서 상향 열기·좌우 step 매핑을 Robolectric으로 검증한다. 실제 손가락 동작의 체감은 실기기 미검증이다.

## 0.1.0-dev14 검증 범위

현재 앱 창의 너비 600dp 이상·높이 480dp 이상일 때 2열로 전환한다. 일반 휴대폰 가로 화면은 높이 조건으로 기존 단일 화면을 유지한다. 방송 목록은 40% 너비(최소 280dp·최대 420dp), 재생 본문·버튼은 최대 680dp, 하단 컨트롤은 최대 1120dp로 가운데 정렬한다. 목록과 상세의 ScrollView 및 편성표 갱신 대상은 분리한다. 재생 서비스와 연결을 재생성하지 않고 화면만 재구성한다.

Robolectric에서 412×915·915×412dp 휴대폰, 673×841·841×673dp 폴드, 800×1280·1280×800dp 태블릿 크기를 검사한다. 열 분리, 본문 폭 제한, 설명 보존·독립 스크롤·버튼 고정, 편성표와 목록의 독립 정렬, 회전·접기 시 선택/정렬/검색/목록 위치 보존, 뒤로가기와 상세 영역 스와이프를 검증한다. 실제 폴드·태블릿 렌더링과 재생 중 물리 회전은 실기기 미검증이다.

DEV14 최종 결과: assembleDebug·lintDebug 통과, 자동 검사 64개 통과·실시간 API 검사 1개 생략. 선택 채널 별표의 양쪽 동기화와 복원 pre-draw 전 재구성 시 목록 위치 보존도 회귀 검사한다. 실제 단말의 화면·제스처 체감은 미검증이다.

## 0.1.0-dev15 목록 위치 보존

populate의 항목 교체 전 스크롤 위치를 저장하고 레이아웃 후 복원한다. 연속 갱신에서는 세대번호와 대상 ScrollView 정체성으로 오래된 콜백을 무효화하고, 복원 대기 중인 저장값을 유지한다. build는 compact에서도 위치를 보존하며 정렬·검색의 명시적 변경만 0으로 초기화한다. 저장상태 생성 시 복원 대기값도 보존한다.

StationListPositionTest는 실제 행 클릭 경로를 사용하고 네트워크 재생만 대체한다. 휴대폰·태블릿 전체/즐겨찾기에서 선택·연속 populate·build 후 위치가 동일한지 검사하고 정렬이 과거 복원 콜백을 무효화하는지 검사한다. assembleDebug·lintDebug 통과, 자동 검사 69개 통과, 실시간 API 검사 1개 생략. 실제 단말의 재생 중 터치·스크롤은 미검증이다. 범위는 위치 보존 및 회귀검사로 한정한다.

## 0.1.0-dev16 시스템 다크모드

기준선은 dev15의 4d27c5e740cc27ed8470075eb9eb88e24a75024a입니다. 시스템 uiMode의 야간 설정과 values/values-night 리소스로 목록·즐겨찾기·재생·편성표·대화상자·시스템 바·진행바·터치 피드백을 전환합니다. 테마 변경 시 이전 테마의 화면 캡처를 폐기하고 대화상자를 새 테마로 재구성합니다. 재생 서비스·미디어 연결·네트워크·권한·앱 데이터 계약은 변경하지 않았습니다. 별도 테마 메뉴를 추가하지 않았습니다.

동일 후보에서 assembleDebug·lintDebug를 통과했고 Android Lint 오류는 0개입니다. 자동 검사 76개 통과, 명시적 실시간 API 검사 1개 생략입니다. 신규 SystemThemeTest 7개는 어두운 모드 시작, 밝음→어두움→밝음 전환, 선택·검색·정렬·즐겨찾기·연결 횟수 유지, 휴대폰 목록과 태블릿 즐겨찾기 위치 보존, 대화상자 재구성, 기본·보조·강조 글자/진행바 대비, 이전 화면 캡처 폐기를 검사합니다. 기존 레이아웃·제스처·목록 위치·미디어 세션 검사를 함께 통과했습니다. 검증 방식은 SELF_CHECK입니다.

기존 kr.jinsu.radio.dev 및 개발 서명 인증서를 유지하고 versionCode를 15에서 16으로 증가시켰습니다. aapt identity, apksigner 서명/이전 인증서 일치, zipalign과 최종 복사 파일 SHA-256을 확인했습니다. ADB 연결 기기가 없어 실제 단말의 설치·테마 전환·음성 연속 재생·시각 체감은 미검증이며 Robolectric 검사를 실기기 결과로 대신하지 않습니다.

범위는 시스템 다크모드와 필요한 리소스·버전·회귀 테스트·문서에 한정했습니다. PR-003에 따라 dev15 버전 문자열 참조를 먼저 검색하고 현재 산출물 정보와 앱 정보를 dev16으로 정합화했습니다. 과거 검증 이력의 버전명은 보존했습니다. 공식 참조: https://developer.android.com/develop/ui/views/theming/darktheme .

## 0.1.0-dev17 연합뉴스TV 유튜브 라이브 오디오

기준선은 dev16 fcc407b8fa5d2d6a8f992cafe1bbaad5455e50b3입니다. 공식 라이브 6QZ_qc75ihU와 공식 channelId UCTHCOPwqNfZ0uiKOvFyhGwg를 현재 공개 응답으로 확인했습니다. 공개 watch 응답의 일회성 guest visitor와 player API로 매 재생/재개 시 새 HLS를 얻고, TYPE=AUDIO rendition 234를 우선 선택합니다. 영상 혼합 스트림으로 대체하지 않으며 서비스의 영상 track도 비활성화했습니다. 기존 링크 종료 시 공식 채널의 현재 live 페이지로 한 번 재탐색합니다. 로그인·쿠키·키·중계서버·의존성 추가는 없습니다.

모든 정렬에서 새 행은 최하단입니다. 즐겨찾기와 이전/다음 순환에 포함하고 가짜 FM 주파수 대신 LIVE만 표시합니다. 목록·재생·미니바·태블릿에서 편성표·프로그램 설명·시간·진행률 및 정보 미제공 문구를 표시하지 않습니다. 기존 FM 14개·다크모드·목록 위치와 출력 세대/취소 ticket 보호는 보존했습니다.

독립 코드/보안 검토에서 signed URL 만료 후 복구와 재개 중 취소 UX를 확인하고 보완했습니다. 앱 재개는 기존 play(selected) 커맨드/future/loading 경로를 사용하고, 외부 재개도 새 주소를 발급받습니다. 오류 복구는 유효한 출력 세대·재생 희망·미진행 조건에서 monotonic clock 기준 60초당 최대 1회이며, 실제 재생 성공 때만 화면 오류를 해제합니다. 빠른 중지·채널 교체·블루투스 분리 이후 오래된 요청은 재생되지 않습니다.

최종 후보의 assembleDebug·lintDebug(오류 0개)·testDebugUnitTest를 통과했습니다. 96개 중 95개 통과·기존 전체 FM 실시간 편성 검사 1개 생략입니다. 새 실제 YouTube live 검사는 명시적으로 켜서 앱의 YoutubeLiveAudio.resolve 자체로 media playlist를 성공 수신했습니다. 해당 경로의 미디어 segment 80,540 bytes를 FFprobe로 검사하여 AAC audio stream 1개·video 0개를 확인했습니다. UI·정렬·즐겨찾기·순환·오디오 manifest 선택·혼합영상 거부·HTTPS/host·공식 live 확인·복구 제한·기존 FM/다크모드 회귀 검사를 포함합니다.

dev16→dev17 applicationId 및 서명 인증서 일치, versionCode 증가(16→17), minSdk 26/targetSdk 36, zipalign과 최종 복사 APK byte/SHA 일치를 확인했습니다. 검증 모드는 INDEPENDENT이며 별도 검증자가 현재 diff·실제 live/codec·최종 테스트 원본을 검토했습니다. 실제 Android 단말 설치·스피커 출력·물리 블루투스 조작·6시간 이상 연속 재생은 ADB 기기 부재로 미검증입니다. 공개 미디어 데이터 검증을 실기기 청취 성공으로 표시하지 않습니다.

범위는 요청한 연합뉴스TV live audio와 필요한 수명/취소 처리·UI·회귀시험·버전/문서입니다. 외부 입력은 고정된 공식 채널만 허용하고 API/redirect/media는 YouTube·Googlevideo HTTPS로 제한하며 12초 타임아웃·5MB 응답 상한을 적용합니다. visitor 및 서명된 주소는 소스·장기 저장·로그에 넣지 않습니다. PR-003/008에 따라 identity 참조 검색 및 실제 공개 응답 구조를 선행 확인했습니다. 공개 player 응답 구조 변경에 따른 추후 추출기 유지보수 가능성은 남습니다.

## 0.1.0-dev18 사용자 라이브 등록·수정과 3탭

사용자 요청은 YTN(aZyD6EPl6KU)·MBC뉴스(q9bM12ucTIY) 추가, 채널명/주소 직접 등록 메뉴, 기존 라이브의 재생화면 주소 수정, 라디오/유튜브 라이브 탭 분리와 공통 즐겨찾기입니다. 기준 소스는 dev17 344c54e1605f63bc03bf53ca51b4c2b8e2bff613입니다. 라디오 14개, 기본 라이브 3개(연합/YTN/MBC), 사용자 등록순 목록을 분리했습니다. 탭 전환만으로 재생을 바꾸지 않고 일반 채널 이동은 현재 종류, 즐겨찾기는 공통 pool을 순환합니다. live 탭의 FM 정렬 헤더를 숨깁니다.

LiveStationStore는 stable id/이름/공개 영상 id/채널 id만 SharedPreferences에 원자적으로 저장합니다. 기본 live는 id를 유지한 override, 신규 live는 user_UUID입니다. 중복 영상·잘못된 이름·보호된 FM id·손상된 기존 저장소를 거절하며 실패 시 원본을 덮어쓰지 않습니다. 기존/등록 live는 재생화면에서 수정하고 등록 항목은 삭제할 수 있습니다. 서비스 단독 시작도 같은 catalog를 읽습니다. 저장만으로 자동 재생하지 않으며 선택 항목 수정/삭제는 pause→stop→clear로 재생 희망값까지 해제합니다.

입력 URL은 HTTPS YouTube 호스트와 영상/채널-live 경로로 정규화하고 실제 공개 현재 live·채널 정체성과 audio-only HLS를 확인합니다. Googlevideo HTTPS/리디렉션/크기/시간 제한은 보존했습니다. 검증 중 이름/주소/버튼을 잠그며 취소·onPause·회전·새 dialog·dead/ticket mismatch는 저장을 막습니다. 회전과 상태 복원은 입력 초안을 유지하고 자동 재검증하지 않습니다. service는 resolve 결과의 id/code/channel/name을 현재 catalog와 대조하여 수정/삭제 전 구주소의 늦은 응답을 취소하고 pending future를 종료합니다.

최종 ONRADIO_REG_STOPFIX_20261103_11에서 assembleDebug·lintDebug(오류 0개)·전체 unit/Robolectric 검사 112개를 통과했습니다. 2개 명시적 네트워크 검사는 이 최종 회귀에서 생략했습니다. generic resolver/catalog가 동일한 첫 후보 ONRADIO_REG_BUILD_20261103_06의 실제3개 inspect→audio media playlist 성공 증거와 ONRADIO_REG_AUDIOPROOF_20261103_07의 AAC-only 세그먼트 검사를 재사용합니다. 각 segment는 연합 80,645바이트 / YTN 81,872바이트 / MBC 81,341바이트이며 모두 AAC audio 1개·video 0개입니다. 첫 live 검사 XML도 별도 보존했습니다.

등록/재실행·기본 주소 수정·stable id/즐겨찾기 유지·중복/손상 저장소·삭제 후 늦은 수정 거부·URL 유사호스트/credentials/port/경로 거부·FM 보호·3탭/공통 즐겨찾기·종류별 순환·취소/onPause/회전/새창 늦은 결과·검증 중 잠금 및 실패 복원·360dp 단일행 48dp 탭·live 헤더 숨김을 검사합니다. 실제 ExoPlayer의 playWhenReady=true/item1에서 수정/삭제 helper 후 wanted=false/item0/IDLE을 확인했습니다. 기존 FM·다크모드·반응형·목록 위치·미디어 세션 회귀도 통과했습니다.

독립 검증자가 실제 diff/테스트/빌드·라이브·코덱 원본을 대조했고, 지적된 입력 변경 경쟁·오해를 주는 live 정렬 헤더·stop 이후 wanted 잔존 문제를 수정했습니다. 검증 모드는 INDEPENDENT입니다. dev17과 동일 applicationId/서명 및 versionCode 17→18 증가, minSdk26/targetSdk36, apksigner·zipalign·canonical/전달 APK 바이트 일치를 확인했습니다. ADB 실기기 설치·장시간 청취·물리 블루투스 조작은 미검증이며 Robolectric/실제 미디어 수신 검증과 구분합니다. 개인정보·계정·추가 권한·의존성·중계서버 확대는 없습니다.

DEV18 independent review: PASS_WITH_RESIDUAL_RISK; security PASS; scope IN_SCOPE. The reviewer directly read the final build result and reviewed the final source, including input locking, live-only header, and pause-before-stop/clear corrections. Physical installation, extended listening, and physical Bluetooth remain unverified.

## 0.1.0-dev19 일반 입력창의 자동완성 제외

사용자가 일반 라이브 등록창에서 삼성패스 등록 안내가 표시된다고 보고했습니다. dev18에서는 일반 EditText의 autofill importance가 기본값이어서 자동완성 서비스의 추정 대상이었습니다. Android 공식 Autofill 최적화 계약에 따라 Activity와 별도 AlertDialog의 최상위 View를 NO_EXCLUDE_DESCENDANTS로, 검색·채널명·라이브 URL을 NO로 지정했습니다. 이름·주소 입력형식·붙여넣기·등록 데이터·재생 경로·즐겨찾기와 기기 전역 설정은 변경하지 않았습니다.

SELF_CHECK / SECURITY_DELTA=NONE / IN_SCOPE. MainActivity의 변경분을 기준 소스와 재구성해 일치시켰으며 입력창 설정 5곳과 버전 표시만 변경했습니다. 빌드·린트 통과, 전체 116개 중 114개 통과·실제 네트워크 선택 검사 2개 미실행입니다. 신규 회귀시험은 API26/28에서 일반 입력창의 자동완성 제외, 등록·수정·포커스 이동·화면 재구성 후 초안 복원, 일반 텍스트/URI 입력형식을 검사했습니다. 첫 시험의 single-line 변환을 비밀번호 변환으로 취급한 assertion은 실제 비밀번호 변환 여부 검사로 정정했습니다. API35 Robolectric는 Linux aarch64 native runtime 미지원으로 실행되지 않았으며 이 결과를 제품 오류나 통과로 표시하지 않습니다. 실제 삼성패스 서비스의 안내가 사라지는지에 대한 단말 관찰은 미검증입니다.

dev18과 동일 applicationId·서명, versionCode18→19, minSdk26/targetSdk36, apksigner와 zipalign을 확인했습니다. 전달 후보 OnRadio-v0.1.0-dev19.apk, 4718676 bytes, SHA-256 a52ca3e65dc72bafd4b0ca76d0faab85c27c5a4cb6ecfb906c0ff034855ea860.

## 0.1.0-dev20 라이브 종료 안내와 등록 영상 고정

사용자 요청: 등록된 라이브가 종료되어 일반 영상으로 전환됐을 때 재생 시 안내합니다. 한 채널에 다수의 라이브가 있을 수 있으므로 채널 단위 자동 탐색으로 후속 방송을 임의 대체하지 않습니다. watch 영상 ID를 저장값→초기 페이지→player API 양쪽에 대조하고 기존 channel/live fallback을 제거했습니다.

수정 전 YouTube의 공개 player 원문을 실제 조회해 NASA 아카이브 CMLD0Lp0JBg(현재 isLive 키 없음, isLiveContent=true, 과거 endTimestamp), 현재 연합뉴스TV 라이브(isLive/isLiveNow=true), 일반 영상(isLiveContent=false)을 비교했습니다. 필요한 공개 필드만 정적 fixture로 보존했습니다. 과거 종료 시각의 구조화 근거가 있을 때 ENDED, 명시적인 현재 live는 LIVE, 시작 전은 UPCOMING, 일반 영상은 NOT_LIVE, 비공개·삭제·접속 실패·누락·잘못된 시간은 UNKNOWN으로 처리합니다. 상태 미확인을 종료로 표시하지 않고 오디오 전용 검증 전에는 재생하지 않습니다.

종료 안내는 typed SessionResult extras로 대상 id/video를 포함해 앱 foreground의 액션 Dialog(주소 수정)로 전달합니다. 화면 밖에서는 대상명을 포함한 Toast를 사용하며 백그라운드 Activity에 Dialog를 띄우지 않습니다. 같은 실패 대상의 확정된 not-live 상태만 재생 의도를 해제하고, 다른 방송이나 일시적 네트워크 실패 시 기존 재생은 보존합니다. 일반 접속 실패 안내는 앱 요청에서도 Toast를 유지합니다. 취소·새 요청·주소 변경·오디오 출력 세대 변경 이후의 늦은 결과와 오래된 수정 액션을 무효화합니다.

ONRADIO_ENDED_REFINEBUILD_20261103_08: 빌드·린트 성공, 전체 137개 중 135개 통과·명시적 외부 네트워크 검사 2개 미실행, 실패 0개입니다. 회귀검사는 현재/종료/일반/예정/미확인 분류, 같은 채널의 다른 초기/API 영상 거부, 페이지와 API 사이 종료, 네트워크 실패 시 다른 라이브 탐색 금지, 정확한 오디오 선택, 실제 PlaybackService/ExoPlayer 상태·타입 전달·Toast·취소 경합, Activity Dialog/주소 수정 버튼과 늦은 액션·화면 이탈 처리를 포함합니다. 초기 UI 시험은 AlertDialog 비동기 button Handler를 진행하지 않은 시험 코드가 원인이었으며 실제 이벤트 loop를 진행하도록 바로잡았습니다.

동일 YoutubeLiveAudio 후보의 ONRADIO_ENDED_TESTBUILD_20261103_06에서 기본 공개 라이브 3개 모두 실제 오디오 media playlist 수신에 성공했습니다(PUBLIC_AUDIO_PLAYLIST_OK yonhap/ytnlive/mbcnews). _06의 UI 시험 1개 실패를 전체 성공으로 표시하지 않으며 해당 네트워크 성공 증거만 보존해 재사용했습니다. 기존 오디오 rendition/영상 차단·즐겨찾기·입력 자동완성 제외는 보존합니다. 실제 단말 설치·종료 순간의 청취·물리 블루투스 동작은 미검증입니다.

dev19와 applicationId/서명 인증서 일치, versionCode19→20, minSdk26/targetSdk36, apksigner·zipalign·canonical/전달 APK 바이트 일치를 확인했습니다. OnRadio-v0.1.0-dev20.apk, 4795494 bytes, SHA-256 ccf14cdfbfb299c09e310958e026d27cfeabf72d0f9235c88ed96ae4b60df35a.

DEV20 independent review: INDEPENDENT=PASS_WITH_RESIDUAL_RISK; SECURITY_SCREENING=PASS; SCOPE=IN_SCOPE. Independent reviewer read final source, _08 terminal build result, latest 137-test XML and preserved _06 public-live evidence. No blocking findings remain. Physical device screen-off/hardware resume end-to-end remains unverified.
