# -*- coding: utf-8 -*-
"""Offline build: javac -> package -> RAT remap to SRG.

Requirements: Java 17, a local ForgeGradle cache, and Minecraft libraries.
Set these environment variables when needed:
    COC_GRADLE_CACHE  (default: ~/.gradle/caches/forge_gradle)
    COC_GAME_LIBS     (Minecraft libraries directory)
    COC_TARGET_MODS   (optional; if set, the jar is copied there)

Usage:
    python build_manual.py --no-deploy
    python build_manual.py            # deploys only when COC_TARGET_MODS is set
"""
import glob
import os
import shutil
import subprocess
import sys
import zipfile

PROJECT = os.path.dirname(os.path.abspath(__file__))
GRADLE_CACHE = os.environ.get(
    "COC_GRADLE_CACHE", os.path.join(os.path.expanduser("~"), ".gradle", "caches", "forge_gradle"))
GAME_LIBS = os.environ.get("COC_GAME_LIBS", "")
TARGET_MODS = os.environ.get("COC_TARGET_MODS", "")

MAPPED_FORGE = os.path.join(
    GRADLE_CACHE,
    r"minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.3.0_mapped_official_1.20.1"
    r"\forge-1.20.1-47.3.0_mapped_official_1.20.1.jar")
MAPPINGS = os.path.join(
    GRADLE_CACHE,
    r"minecraft_user_repo\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412"
    r"\srg_to_official_1.20.1.tsrg")
RAT = os.path.join(
    GRADLE_CACHE,
    r"maven_downloader\net\minecraftforge\ForgeAutoRenamingTool\1.0.2"
    r"\ForgeAutoRenamingTool-1.0.2-all.jar")

MOD_ID = "coc"
MOD_VERSION = "2.0.0"
JAR_NAME = f"{MOD_ID}-{MOD_VERSION}.jar"

MAIN_JAVA = os.path.join(PROJECT, "src", "main", "java")
RESOURCES = os.path.join(PROJECT, "src", "main", "resources")
OUT = os.path.join(PROJECT, "build")


def cp_join(paths):
    return ";".join(p.replace("\\", "/") for p in paths)


def collect_java(root):
    return sorted(s.replace("\\", "/") for s in
                  glob.glob(os.path.join(root, "**", "*.java"), recursive=True))


def get_classpath():
    if not os.path.isfile(MAPPED_FORGE):
        raise SystemExit("缺少官方名 Forge 合并 jar: " + MAPPED_FORGE)
    jars = [MAPPED_FORGE]
    # ForgeGradle 缓存里的 MC 库/依赖
    for p in glob.glob(os.path.join(GRADLE_CACHE, "maven_downloader", "**", "*.jar"), recursive=True):
        jars.append(p)
    # 游戏自带的 Forge/FML 运行库（避免 1.12.2、旧 47.4.0/47.4.13 干扰）
    for p in glob.glob(os.path.join(GAME_LIBS, "net", "minecraftforge", "**", "*.jar"), recursive=True):
        if "1.12.2" in p or "47.4.0" in p or "47.4.13" in p:
            continue
        jars.append(p)
    # 去重，保留顺序
    seen = set()
    result = []
    for p in jars:
        key = os.path.normcase(os.path.abspath(p))
        if key not in seen:
            seen.add(key)
            result.append(p)
    return result


def compile_sources(sources):
    classes = os.path.join(OUT, "classes")
    shutil.rmtree(classes, ignore_errors=True)
    os.makedirs(classes, exist_ok=True)
    cp = cp_join(get_classpath())
    args_file = os.path.join(OUT, "javac-args.txt")
    with open(args_file, "w", encoding="utf-8", newline="\n") as f:
        f.write("-encoding UTF-8\n")
        f.write("-source 17\n-target 17\n")
        f.write("-proc:none\n-nowarn\n")
        f.write("-d " + classes.replace("\\", "/") + "\n")
        f.write("-cp " + cp + "\n")
        for s in sources:
            f.write(s + "\n")
    print(f"[compile] {len(sources)} 个源文件, classpath {len(cp)} 字符")
    result = subprocess.run(["javac", "@" + args_file], capture_output=True, text=True,
                            encoding="utf-8", errors="replace")
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        raise SystemExit("[compile] 编译失败")
    print("[compile] 通过")
    return classes


def package_dev_jar(classes):
    dev_jar = os.path.join(OUT, JAR_NAME.replace(".jar", "-dev.jar"))
    if os.path.exists(dev_jar):
        os.remove(dev_jar)
    with zipfile.ZipFile(dev_jar, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF",
                   "Manifest-Version: 1.0\r\n"
                   "Specification-Title: coc\r\n"
                   "Implementation-Title: coc-1.20.1\r\n"
                   f"Implementation-Version: {MOD_VERSION}\r\n\r\n")
        for root, _, files in os.walk(classes):
            for name in files:
                p = os.path.join(root, name)
                z.write(p, os.path.relpath(p, classes).replace("\\", "/"))
        for root, _, files in os.walk(RESOURCES):
            for name in files:
                p = os.path.join(root, name)
                z.write(p, os.path.relpath(p, RESOURCES).replace("\\", "/"))
    print("[package] dev jar:", dev_jar, f"{os.path.getsize(dev_jar)/1024:.1f} KB")
    return dev_jar


def remap(dev_jar):
    final_jar = os.path.join(OUT, JAR_NAME)
    if os.path.exists(final_jar):
        os.remove(final_jar)
    cmd = ["java", "-jar", RAT,
           "--input", dev_jar,
           "--output", final_jar,
           "--map", MAPPINGS,
           "--reverse",
           "--lib", MAPPED_FORGE]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        raise SystemExit("[remap] 重映射失败")
    print("[remap] 最终 jar:", final_jar, f"{os.path.getsize(final_jar)/1024:.1f} KB")
    return final_jar


def deploy(final_jar):
    if not TARGET_MODS:
        print("[deploy] COC_TARGET_MODS is not set; skipping deployment")
        return
    os.makedirs(TARGET_MODS, exist_ok=True)
    for old in glob.glob(os.path.join(TARGET_MODS, "coc-*.jar")):
        try:
            os.remove(old)
            print("[deploy] 移除旧版本:", old)
        except OSError as e:
            print("[deploy] 无法移除旧版本（可能被游戏占用）:", old, e)
    dest = os.path.join(TARGET_MODS, os.path.basename(final_jar))
    shutil.copy2(final_jar, dest)
    print("[deploy] 已部署:", dest)


def main():
    os.makedirs(OUT, exist_ok=True)
    sources = collect_java(MAIN_JAVA)
    if not sources:
        raise SystemExit("没有源文件")
    classes = compile_sources(sources)
    dev_jar = package_dev_jar(classes)
    final_jar = remap(dev_jar)
    if "--no-deploy" not in sys.argv and TARGET_MODS:
        deploy(final_jar)
    elif "--no-deploy" not in sys.argv:
        print("[deploy] COC_TARGET_MODS is not set; skipping deployment")
    print("[done] coc 构建完成")


if __name__ == "__main__":
    main()
