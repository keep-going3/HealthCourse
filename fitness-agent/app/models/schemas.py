from pydantic import BaseModel, Field
from typing import Optional


class ChatRequest(BaseModel):
    userId: int
    message: str
    context: Optional[dict] = None


class ParseWorkoutRequest(BaseModel):
    userId: int
    text: str


class ParseWorkoutResponse(BaseModel):
    code: int = 200
    message: str = "success"
    data: Optional[dict] = None


class ChatContext(BaseModel):
    userInfo: Optional[dict] = None
    recentWorkouts: Optional[list] = None
    weightRecords: Optional[list] = None
    chatHistory: Optional[list] = None


class ExerciseSet(BaseModel):
    setNumber: int
    weightKg: Optional[float] = None
    reps: Optional[int] = None


class ParsedExercise(BaseModel):
    exerciseName: str
    standardName: Optional[str] = None
    confidence: float = 0.0
    sets: list[ExerciseSet] = []


class ParsedWorkoutData(BaseModel):
    sessionDate: str
    bodyParts: list[str] = []
    notes: str = ""
    exercises: list[ParsedExercise] = []
