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

DEV9 결과: build-a assembleDebug/lintDebug/testDebugUnitTest 성공. 제품 소스 변경 없이 controller readback 테스트만 보강한 controller-test에서 총 40건 통과, 실시간 API 1건 기본 제외. Robolectric 9건에는 앱 화면 없는 미디어 세션 연결, 표시 duration=3600000ms/position=1800000ms, paused 30초 경과→1830000ms readback, seek 명령 차단 및 raw engine 위치 불변을 포함한다. ProgramProgress 5건은 반 진행·시작/종료 clamp·유효하지 않은 편성·자정·종료 경계를 검증한다. 독립 검토에서 virtual timeline/단위 변환/읽기 전용/라이브 복귀/이벤트 재귀·작업 누적 경로를 확인했다. native apksigner/zipalign 및 기존 개발 서명 일치 확인. 실제 휴대폰 알림·잠금화면의 표시 모양은 미검증이다.
