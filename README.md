# 온라디오

국내 FM 14개 채널과 연합뉴스TV의 유튜브 라이브 오디오를 인터넷 스트리밍으로 듣는 간결한 Android 앱입니다.

방송 목록과 검색, 기기에 보존되는 즐겨찾기, 스크롤과 독립된 하단 컨트롤러, 별도 재생화면을 제공합니다. 목록 상단의 주파수·방송국명 열 제목으로 정렬하고 재터치하면 방향이 전환됩니다. 기본 글자색 화살표는 현재 정렬 열에만 표시하며 기준과 방향을 기기에 저장합니다. 편성표를 열면 현재 프로그램을 가운데로 스크롤합니다. 재생화면에 채널/주파수/현재 프로그램/방송시간/프로그램 소개를 표시합니다. KBS·MBC·SBS·CBS·EBS·TBS·YTN의 공식 편성 데이터로 한국시간 당일 편성을 조회합니다. 목록에도 현재 프로그램명과 방송시간을 표시합니다. 프로그램 정보를 조회하지 못하면 조회 상태를 표시합니다. 화면을 꺼도 Media3 세션 서비스에서 재생하며 미디어 알림·오디오 포커스·이어폰 분리를 지원합니다.

방송 주소는 KBS·MBC·SBS의 공식 스트림 발급 API에서 매번 새로 받습니다. 계정, 자체 중계서버, 유료 API, 녹음 기능은 없습니다. FM 수신칩을 사용하지 않으며 통신 데이터가 필요합니다.

## 빌드

JDK 17 이상, Android SDK 36, Gradle 8.14.5, Android Gradle Plugin 8.9.1.

```sh
./gradlew :app:assembleDebug
```

Termux에서는 `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`를 지정합니다. DEV applicationId는 `kr.jinsu.radio.dev`입니다. 재배포 시 기존 개발 서명을 유지하고 versionCode를 증가시킵니다. 개인 서명키는 저장소에 포함하지 않습니다.

## 자료

- https://developer.android.com/media/media3/session/background-playback
- KBS: https://cfpwwwapi.kbs.co.kr/ 및 https://static.api.kbs.co.kr/
- MBC: https://mini.imbc.com/ 및 https://sminiplay.imbc.com/
- SBS: https://apis.sbs.co.kr/ 및 https://static.cloud.sbs.co.kr/
- CBS: https://www.cbs.co.kr/ 및 https://m-aac.cbs.co.kr/
- TBS: https://tbs.seoul.kr/ 및 https://cdnfm.tbs.seoul.kr/
- YTN: https://radio.ytn.co.kr/ 및 https://radiolive.ytn.co.kr/
- EBS: https://ebsonair.ebs.co.kr/

디자인 참고: Unslop, Damir Sokolovsky (@heydamir), CC BY 4.0, https://github.com/heydamir/unslop . 방송사와 제휴 관계가 없는 개인용 클라이언트입니다.

블루투스 이어폰 해제 시 재생 중지: Media3 오디오 출력 전환 처리와 Bluetooth A2DP·SCO·LE Audio·보청기 출력 제거 콜백을 함께 적용한다. 마지막 Bluetooth 출력이 제거되면 재생과 스트리밍을 중지하고, 연결 해제 전 시작된 방송 주소 조회가 나중에 완료돼도 스피커로 자동 재생하지 않는다. 재연결만으로 자동 재생하지 않으며 Bluetooth 연결 권한을 새로 요구하지 않는다. 실제 이어폰 연결·해제는 실기기 미검증 항목이다.

재생바 터치 시 재생화면 전체를 420ms 동안 아래에서 위로 전환하고 뒤로가기 시 같은 거리로 내려간다. 하단 재생바는 움직이지 않고, 시스템 애니메이션 비활성 설정을 존중한다. 화면 이동으로 분리된 View의 중앙이동 콜백은 즉시 해제한다. 재생 요청에는 오디오 출력 세대값을 실어 서비스의 실제 play/setPlayWhenReady 시점에도 연결 해제 이전 요청을 거부한다.

폴드 펼침 화면과 태블릿의 가로·세로에서는 방송 목록과 재생정보를 나란히 배치한다. 편성표는 오른쪽에 열리며 목록과 독립적으로 스크롤한다. 설명 길이와 무관하게 편성표·홈페이지 버튼은 상세 영역 하단에 고정한다. 창 크기를 바꿀 때 선택 방송·정렬·검색·목록 위치를 보존한다. 넓은 화면의 본문과 하단 컨트롤은 최대 너비를 제한한다.

전체 방송·즐겨찾기 목록에서 채널 선택과 연결 완료로 항목이 갱신되어도 현재 스크롤 위치를 유지한다. 검색·정렬 기준을 직접 변경하면 결과 처음으로 이동한다.


시스템 다크모드를 자동으로 따릅니다. 전체 방송·즐겨찾기·재생·편성표·앱 정보와 상태바·탐색바에 밝은/어두운 색상을 적용합니다. 실행 중 시스템 테마가 바뀌어도 선택 방송·즐겨찾기·검색·정렬·목록 위치를 보존하고 재생 서비스를 재시작하지 않습니다. 별도 테마 선택 메뉴는 없습니다.

## 연합뉴스TV 실시간 오디오

연합뉴스TV 공식 유튜브 라이브(최초 videoId: 6QZ_qc75ihU)를 모든 정렬에서 목록 맨 아래에 표시합니다. 즐겨찾기·이전/다음·알림·잠금화면·백그라운드는 기존 Media3 라디오 경로를 공유합니다. FM 주파수를 만들지 않고 LIVE로 표시하며 편성표·프로그램 설명·방송시간·진행률·정보 미제공 문구는 표시하지 않습니다.

재생 시 공개 플레이어 응답에서 오디오 전용 HLS rendition만 선택하고 영상 트랙은 비활성화합니다. 새로 재생하거나 일시정지 후 재개하면 주소를 다시 발급받습니다. 기존 방송 링크가 종료되면 연합뉴스TV 공식 채널의 현재 라이브를 다시 확인합니다. 재생 오류는 사용자가 계속 재생을 요청한 상태와 유효한 오디오 출력 세대에서만 60초당 최대 1회 새 주소로 복구하며, 중지·블루투스 해제·새 채널 선택 이후 이전 요청은 재생되지 않습니다.

YouTube·Googlevideo의 HTTPS 공개 응답만 사용합니다. 로그인·쿠키·API 키·중계서버·새 라이브러리는 추가하지 않습니다. 접속용 visitor 값과 만료 미디어 주소는 메모리에서만 사용합니다. YouTube의 공개 재생 응답 구조나 방송 제공 정책이 바뀌면 추출 구현의 후속 수정이 필요할 수 있습니다.
