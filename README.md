# Aimless - Paper 26.3

플레이어끼리 **말로 소통할 수 없고, 누가 접속해 있는지도 알 수 없는** 서버를 만드는 마인크래프트 서버 플러그인입니다.

원작: [noonmaru/aimless](https://github.com/noonmaru/aimless) (Noonmaru, Paper 1.16) 를 Paper 26.3 용으로 이식했습니다.

## 서버 실행

1. [Java 25 이상](https://adoptium.net/) 설치
2. 이 저장소의 `server.ps1`, `server.bat` 을 빈 폴더에 받기
3. `server.bat` 더블클릭

Paper 서버와 Aimless 플러그인(Releases 의 최신 버전)이 자동으로 설치되고 서버가 시작됩니다.
(`server.ps1` 옆에 `aimless` 소스 폴더가 있으면 Releases 대신 직접 빌드하며, 이때는 JDK 25 이상이 필요합니다.)

## 규칙

- **채팅 금지** - 감정 표현 단어만 머리 위 파티클로 표시됩니다.
- **명령어 금지** - OP 를 포함한 모든 플레이어 (서버 콘솔만 사용 가능), 탭 자동완성도 차단
- **표지판 / 책** - 스폰(0, 0) 주변 32×32 밖에서는 글자·숫자·기호 구분 없이, 길이와 상관없이 `?????` 로 바뀝니다 (표지판은 한 줄, 책은 한 페이지/제목 단위).
- 접속/퇴장/업적 메시지 없음, 사망 메시지는 `사람이 죽었다.`
- Tab 목록에 아무도 표시되지 않습니다 (플레이어 얼굴 숨김).
- F3 좌표 숨김, 월드 경계 16384, 처음 접속 / 리스폰 위치는 닉네임으로 정해지는 무작위 위치
- 서버 목록에는 인원 대신 날짜와 시각이 표시됩니다.
- 킥이 모두 무효이며, 서버가 가득 차도 접속할 수 있습니다.
- **2시간마다 자동 재시작** (1분 전 예고). 서버 창에서 `stop` 을 입력하면 완전히 종료됩니다.

### 감정 표현

| 입력 | 효과 |
|---|---|
| `화남` / `angry` | 화난 주민 |
| `하트` / `heart` | 하트 |
| `ㅗ` / `망할` | 하트 |
| `상처` / `damage` | 피해 표시 |
| `많은상처` / `critical` | 피해 표시 여러 개 |
| `퉤` / `spit` | 침 뱉기 + 소리 |
| `하지마` / `no` | 방벽 표시 + 모루 소리 |
| `즐` / `note` | 음표 |
| `열받` / `rage` | 용암 + 폭발음 |
| `눈물` / `tear` | 눈물 |
| `피눈물` / `lava` | 용암 눈물 |
| `눈꼽` / `honey` | 꿀 눈물 |

## 원본(0.1.2)과 다른 점

- Paper 26.3 / Java 25+ 대응, Kotlin 대신 Java
- Kotlin, ProtocolLib 플러그인 의존성 제거
- 표지판/책 제한: 원본은 영어·한글만 한 글자씩 `?` 로 바꿨지만, 이식본은 내용이 있으면 무엇이든 `?????` 로 바꿈
- 서버 목록 설명: `AIMLESS SERVER 2020` → `AIMLESS SERVER 2026`
- 2시간 자동 재시작이 `server.ps1` 과 연동되어 항상 다시 켜짐
- Tab 목록: 원본은 접속한 적 있는 모든 플레이어를 표시했지만, 이식본은 아무도 표시하지 않음
- 게임 규칙 `players_sleeping_percentage` 101: 침대에서 자도 밤이 넘어가지 않음
- 지도에 플레이어 위치 표식이 나오지 않음
- 게임 규칙 `locator_bar` false: 경험치 바 위에 근처 플레이어 방향이 표시되지 않음

## 릴리스 (개발자용)

`v` 로 시작하는 태그를 푸시하면 GitHub Actions 가 플러그인을 빌드해 Releases 에 올립니다.

```
git tag v0.2.0
git push origin v0.2.0
```

## 저작권 및 출처

이 저장소는 [noonmaru/aimless](https://github.com/noonmaru/aimless) 0.1.2 (Noonmaru, GPL-3.0) 의 수정본입니다.
`server.ps1` 은 [monun/inv-captive](https://github.com/monun/inv-captive) (Noonmaru, GPL-3.0) 의 스크립트를 바탕으로 합니다.
2026-11-20 kaneteulin 이 아래와 같이 수정했습니다. 각 파일 상단에도 출처와 수정 내용이 적혀 있습니다.

| 파일 | 출처 | 수정 내용 |
|---|---|---|
| `aimless/src/.../AimlessPlugin.java` | 원작 `AimlessPlugin.kt` | Kotlin → Java, 재시작 표시 파일 추가 |
| `aimless/src/.../EventListener.java` | 원작 `EventListener.kt` | Kotlin → Java, Paper 26.3 이벤트, 표지판/책 제한 변경, MOTD 연도 |
| `aimless/src/.../Emote.java` | 원작 `Emote.kt` | Kotlin → Java, 파티클 이름 갱신 |
| `aimless/src/.../PlayerList.java` | 원작 `PlayerList.kt` | Kotlin → Java, ProtocolLib 대신 26.3 패킷 직접 전송 |
| `aimless/src/main/resources/plugin.yml` | 원작 `plugin.yml` | 의존성 제거 |
| `server.ps1` | inv-captive 의 `server.ps1` | Paper 26.3 다운로드, 플러그인 빌드/다운로드, 자동 재시작 연동 |
| `aimless/build.ps1`, `server.bat`, `.github/` | 새로 작성 | - |

## 라이선스

GPL-3.0 - 원작의 라이선스를 따릅니다. [LICENSE.md](LICENSE.md)
