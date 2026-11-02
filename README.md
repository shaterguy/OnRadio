# 온라디오

국내 FM 14개 채널을 인터넷 스트리밍으로 듣는 간결한 Android 앱입니다.

방송 목록과 검색, 기기에 보존되는 즐겨찾기, 스크롤과 독립된 하단 컨트롤러, 별도 재생화면을 제공합니다. 목록 상단의 주파수·방송국명 열 제목으로 정렬하고 재터치하면 방향이 전환됩니다. 검정색 화살표는 현재 정렬 열에만 표시하며 기준과 방향을 기기에 저장합니다. 편성표를 열면 현재 프로그램을 가운데로 스크롤합니다. 재생화면에 채널/주파수/현재 프로그램/방송시간/프로그램 소개를 표시합니다. KBS·MBC·SBS·CBS·EBS·TBS·YTN의 공식 편성 데이터로 한국시간 당일 편성을 조회합니다. 목록에도 현재 프로그램명과 방송시간을 표시합니다. 프로그램 정보를 조회하지 못하면 조회 상태를 표시합니다. 화면을 꺼도 Media3 세션 서비스에서 재생하며 미디어 알림·오디오 포커스·이어폰 분리를 지원합니다.

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

재생바 터치 시 재생화면 전체를 420ms 동안 아래에서 위로 전환하고 뒤로가기하면 아래 끝까지 내려간다. 하단 재생바는 움직이지 않고, 시스템 애니메이션 비활성 설정을 존중한다. 화면 이동으로 분리된 View의 중앙이동 콜백은 즉시 해제한다. 재생 요청에는 오디오 출력 세대값을 실어 서비스의 실제 play/setPlayWhenReady 시점에도 연결 해제 이전 요청을 거부한다.

하단 재생바 이전/다음은 목록 정렬과 무관하게 주파수 오름차순으로 채널을 이동한다. 즐겨찾기 화면에서는 즐겨찾기 범위, 비어 있으면 전체 채널을 사용하며 양 끝에서는 순환한다.
